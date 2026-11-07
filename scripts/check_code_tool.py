#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V348 — بررسی واقعی ابزار کدنویسی روی Chromium (ابزار محلی؛ در CI اجرا نمی‌شود).
اجرا:  python3 scripts/check_code_tool.py
تصویرها در $V348_OUT (پیش‌فرض /tmp/v348-check).

بررسی می‌کند: انتخاب ۱۲ زبان، باز شدن محیط کد، رنگی‌شدن و شمارهٔ خط و Tab،
درج بلوک کد در سؤال، حالت «پاسخ کدنویسی» برای تشریحی، رفت‌وبرگشت دادهٔ سؤال،
و ایمن‌بودن رندر (کد کاربر نباید HTML اجرا کند)."""
import os
import pathlib
import sys
import tempfile

SCRIPTS = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import mock_supabase_server as mss           # noqa: E402
import web_ui_e2e as e2e                     # noqa: E402
from playwright.sync_api import sync_playwright  # noqa: E402

OUT = pathlib.Path(os.environ.get('V348_OUT', '/tmp/v348-check'))
OUT.mkdir(parents=True, exist_ok=True)
fails = []


def check(name, cond, extra=''):
    print(('  OK   ' if cond else '  FAIL ') + name + (('  → ' + str(extra)) if extra else ''))
    if not cond:
        fails.append(name)


mock_srv, mock_state = mss.start_server_in_thread('127.0.0.1', 0)
mock_url = 'http://127.0.0.1:%d' % mock_srv.server_address[1]
anon = mss.make_public_anon_key()
tmp = tempfile.TemporaryDirectory(prefix='v348_site_')
site_dir = pathlib.Path(tmp.name)
e2e.build_chunked_site_out(site_dir, anon)
web_srv, web_url = e2e.start_readonly_site_server(site_dir, mock_url, anon)
print('site:', web_url, '\n')

with sync_playwright() as pw:
    browser = pw.chromium.launch()
    ctx = browser.new_context(viewport={'width': 1366, 'height': 950})
    page = ctx.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.goto(web_url + '/', wait_until='domcontentloaded')
    page.locator("input[type='text']").first.fill('uitest_teacher')
    page.locator("input[type='password']").first.fill('TeacherPass123!')
    page.locator("input[type='password']").first.press('Enter')
    try:
        page.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)
    except Exception:
        print('teacher password guess failed; trying the mock default')
        page.locator("input[type='password']").first.fill('Passw0rd!123')
        page.locator("input[type='password']").first.press('Enter')
        page.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)
    page.wait_for_function("() => window.ExamSite && window.ExamSite.user()", timeout=30000)
    page.evaluate("() => window.ExamSite.loadPanels()")
    page.wait_for_function("() => window.SiteCode && window.SiteBuilder && window.SiteStudent", timeout=60000)

    # ---------- ۱) درستی ماژول در مرورگر واقعی ----------
    unit = page.evaluate("""() => {
      const C = window.SiteCode, out = {};
      out.langCount = C.LANGS.length;
      out.ids = C.LANGS.map(l => l.id);
      out.aliasPy = C.lang('py').id; out.aliasCpp = C.lang('c++').id; out.aliasCs = C.lang('c#').id;
      out.aliasUnknown = C.lang('klingon').id;
      const f = C.fence('print("hi")\\n', 'python');
      out.fence = f;
      const p = C.parseFence(f);
      out.roundTrip = p && p.lang === 'python' && p.code === 'print("hi")';
      out.hasFence = C.hasFence(f) && !C.hasFence('متن معمولی');
      const mixed = 'قبل\\n```js\\nlet a=1;\\n```\\nبعد';
      const parts = C.split(mixed);
      out.splitKinds = parts.map(x => x.code ? 'code' : 'text');
      out.splitLang = parts.filter(x => x.code)[0].lang;
      // ایمنی: کد کاربر نباید HTML اجرا کند
      const evilCode = '<img src=x onerror=alert(1)>';
      const evil = C.blockHtml(evilCode, 'javascript');
      out.noRawImg = !/<img/i.test(evil);
      const probe = document.createElement('div'); probe.innerHTML = evil;
      out.noInjectedNode = probe.querySelectorAll('img').length === 0;
      out.textRoundTrip = probe.querySelector('.cx-code').textContent === evilCode;
      // رنگی‌شدن واقعاً تولید می‌شود
      const hl = C.highlight('def f(x):\\n    return x + 1  # note', 'python');
      out.hasKeyword = /cx-kw">def</.test(hl);
      out.hasComment = /cx-cm/.test(hl);
      out.hasNumber = /cx-nu/.test(hl);
      const hs = C.highlight('SELECT name FROM t WHERE x=1;', 'sql');
      out.sqlKeywordCase = /cx-kw">SELECT</.test(hs) && /cx-kw">name</.test(hs) === false;
      // بلوک با شمارهٔ خط
      const blk = C.blockHtml('a\\nb\\nc', 'python');
      out.gutter = /cx-num/.test(blk) && /1\\n2\\n3/.test(blk);
      // سه‌بک‌تیک داخل کد بلوک را نمی‌شکند
      const tricky = C.fence('let s = "```";', 'javascript');
      const back = C.parseFence(tricky);
      out.trickyFence = back && back.code === 'let s = "```";';
      return out;
    }""")
    check('۱۲ زبان در فهرست', unit['langCount'] == 12, unit['langCount'])
    check('نام‌های جایگزین کار می‌کنند',
          unit['aliasPy'] == 'python' and unit['aliasCpp'] == 'cpp' and unit['aliasCs'] == 'csharp')
    check('زبان ناشناخته به پیش‌فرض می‌افتد', unit['aliasUnknown'] == 'python')
    check('رفت‌وبرگشت قالب ذخیره‌سازی', unit['roundTrip'], unit['fence'].replace('\n', '\\n'))
    check('تشخیص بلوک کد', unit['hasFence'])
    check('متن و کد از هم جدا می‌شوند', unit['splitKinds'] == ['text', 'code', 'text'], unit['splitKinds'])
    check('زبان بلوک درست خوانده می‌شود', unit['splitLang'] == 'javascript')
    check('کد کاربر HTML اجرا نمی‌کند (ایمن)',
          unit['noRawImg'] and unit['noInjectedNode'] and unit['textRoundTrip'],
          {k: v for k, v in unit.items() if k in ('noRawImg', 'noInjectedNode', 'textRoundTrip')})
    check('کلیدواژه/توضیح/عدد رنگی می‌شوند',
          unit['hasKeyword'] and unit['hasComment'] and unit['hasNumber'])
    check('در SQL بزرگی/کوچی حروف مهم نیست', unit['sqlKeywordCase'])
    check('شمارهٔ خط ساخته می‌شود', unit['gutter'])
    check('سه‌بک‌تیک داخل کد بلوک را نمی‌شکند', unit['trickyFence'])

    # ---------- ۲) سازندهٔ سؤال: ابزار → زبان → محیط ----------
    page.evaluate("() => window.ExamSite.go('builder')")
    page.wait_for_function("() => Array.from(document.querySelectorAll('button')).some(b => b.textContent.trim() === 'تشریحی')", timeout=60000)
    page.evaluate("() => Array.from(document.querySelectorAll('button')).find(b => b.textContent.trim() === 'تشریحی').click()")
    page.wait_for_selector('.b-tools .tool-btn.is-code', timeout=60000)
    check('دکمهٔ ابزار کد در نوار ابزار هست', True)
    page.click('.b-tools .tool-btn.is-code')
    page.wait_for_selector('.cx-grid .cx-tile', timeout=15000)
    tiles = page.eval_on_selector_all('.cx-grid .cx-tile .cx-tile-name', 'e => e.map(x => x.textContent)')
    check('با لمس ابزار، فهرست ۱۲ زبان باز می‌شود', len(tiles) == 12, ' | '.join(tiles))
    page.screenshot(path=str(OUT / 'language-picker.png'))

    page.click('.cx-grid .cx-tile:has-text("پایتون")')
    page.wait_for_selector('.cx-editor .cx-ta', timeout=15000)
    dirs = page.evaluate("""() => {
      const o = {};
      [['pane','.cx-pane'],['num','.cx-num'],['hl','.cx-hl'],['ta','.cx-ta']].forEach(function (pair) {
        const e = document.querySelector(pair[1]);
        o[pair[0]] = e ? (getComputedStyle(e).direction + '/' + getComputedStyle(e).textAlign) : 'MISSING';
      });
      return o;
    }""")
    check('محیط کد چپ‌به‌راست و چپ‌چین است',
          dirs['pane'] == 'ltr/left' and dirs['hl'] == 'ltr/left' and dirs['ta'] == 'ltr/left'
          and dirs['num'].startswith('ltr/'), dirs)
    check('با انتخاب زبان، محیط کدنویسی باز می‌شود', True)
    editor_lang = page.eval_on_selector('.cx-editor .cx-name', 'e => e.textContent')
    check('زبان انتخاب‌شده در نوار محیط نوشته شده', editor_lang == 'پایتون', editor_lang)
    sample = page.eval_on_selector('.cx-editor .cx-ta', 'e => e.value')
    check('نمونهٔ آغاز زبان گذاشته شده', 'def greet' in sample, sample.split('\n')[0])

    ta = page.locator('.cx-editor .cx-ta')
    ta.fill('def add(a, b):\n    return a + b')
    page.wait_for_timeout(150)
    hl_count = page.eval_on_selector_all('.cx-editor .cx-hl .cx-kw', 'e => e.length')
    check('کد نوشته‌شده رنگی می‌شود', hl_count >= 2, hl_count)
    gutter = page.eval_on_selector('.cx-editor .cx-pane .cx-num', 'e => e.textContent')
    check('شمارهٔ خط با تعداد خط‌ها می‌خواند', gutter.strip().split('\n')[-1] == '2', repr(gutter))
    # Tab باید تورفتگی بگذارد نه فوکوس را ببرد
    ta.press('End')
    ta.press('Enter')
    page.wait_for_timeout(80)
    after_enter = page.eval_on_selector('.cx-editor .cx-ta', 'e => e.value')
    check('Enter تورفتگی خط قبل را نگه می‌دارد', after_enter.endswith('\n    '), repr(after_enter[-12:]))
    ta.type('x = 1')
    ta.press('Home')
    ta.press('Tab')
    page.wait_for_timeout(80)
    after_tab = page.eval_on_selector('.cx-editor .cx-ta', 'e => e.value')
    check('Tab تورفتگی می‌گذارد', '        x = 1' in after_tab, repr(after_tab.split('\n')[-1]))
    page.screenshot(path=str(OUT / 'editor.png'))

    ta.fill('def add(a, b):\n    return a + b\n')
    page.click('.cx-editor .cx-foot .btn:not(.light)')
    page.wait_for_timeout(300)
    qtext = page.evaluate("() => Array.from(document.querySelectorAll('textarea')).map(t => t.value).find(v => v && v.indexOf('```') >= 0) || ''")
    check('بلوک کد در متن سؤال درج شد', '```python' in qtext and 'def add' in qtext, qtext.replace('\n', '\\n')[:70])
    page.wait_for_selector('.cx-block', state='attached', timeout=20000)
    diag = page.evaluate("""() => Array.from(document.querySelectorAll('.cx-block')).map(b => {
      var chain = [], n = b;
      while (n && n !== document.body) { var st = getComputedStyle(n); if (st.display === 'none' || st.visibility === 'hidden') chain.push((n.className||n.tagName) + ':' + st.display); n = n.parentElement; }
      return {hiddenBy: chain, inLive: !!b.closest('.b-live')};
    })""")
    print('   تشخیص دید:', diag)
    visible = page.evaluate("() => Array.from(document.querySelectorAll('.cx-block')).filter(b => b.getClientRects().length).length")
    check('پیش‌نمایش سؤال، کد را رنگی و دیدنی نشان می‌دهد', visible > 0, 'visible=%d' % visible)
    badge = page.eval_on_selector('.cx-block .cx-badge', 'e => e.textContent')
    check('نشان زبان روی بلوک هست', badge == 'Py', badge)
    page.screenshot(path=str(OUT / 'question-with-code.png'))

    # ---------- ۳) حالت «پاسخ کدنویسی» برای تشریحی ----------
    has_code_mode = page.evaluate("() => !!Array.from(document.querySelectorAll('.b-type button')).find(b => /کدنویسی/.test(b.textContent))")
    check('گزینهٔ «پاسخ کدنویسی» برای سؤال تشریحی هست', has_code_mode)
    page.evaluate("""() => {
      const b = Array.from(document.querySelectorAll('.b-type button')).find(x => /کدنویسی/.test(x.textContent));
      b.click();
    }""")
    page.wait_for_timeout(400)
    opts = page.evaluate("""() => Array.from(document.querySelectorAll('.b-type button')).map(b => b.textContent).filter(t => /آزاد|ثابت|آغازین/.test(t))""")
    check('تنظیم زبان و کد آغازین پیدا شد', len(opts) >= 2, opts)
    page.evaluate("""() => {
      const b = Array.from(document.querySelectorAll('.b-type button')).find(x => /آزاد/.test(x.textContent));
      b.click();
    }""")
    page.wait_for_selector('.cx-grid .cx-tile', timeout=15000)
    page.click('.cx-grid .cx-tile:has-text("جاوااسکریپت")')
    page.wait_for_timeout(400)
    fixed = page.evaluate("""() => Array.from(document.querySelectorAll('.b-type button')).map(b => b.textContent).find(t => /ثابت/.test(t)) || ''""")
    check('زبان پاسخ دانش‌آموز ثابت می‌شود', 'جاوااسکریپت' in fixed, fixed)
    page.screenshot(path=str(OUT / 'code-mode.png'))

    # ---------- ۴) رفت‌وبرگشت دادهٔ سؤال ----------
    rt = page.evaluate("""() => {
      const B = window.SiteBuilder;
      const q = B.newQuestion('essay');
      q.text = 'تابع را بنویس'; q.mode = 'code'; q.codeLang = 'python'; q.codeStarter = 'def f():\\n    pass';
      const enc = B.encodeQuestions([q]);
      const pub = enc.publicQuestions[0];
      const back = B.decodeQuestion(pub, enc.answerKey[0] || {});
      return {mode: pub.mode, lang: pub.code_lang, starter: pub.code_starter,
              backMode: back.mode, backLang: back.codeLang, backStarter: back.codeStarter};
    }""")
    check('حالت code در دادهٔ سؤال ذخیره می‌شود', rt['mode'] == 'code', rt)
    check('زبان و کد آغازین ذخیره می‌شوند', rt['lang'] == 'python' and 'def f' in (rt['starter'] or ''), rt)
    check('هنگام بازخوانی، حالت و زبان برمی‌گردند',
          rt['backMode'] == 'code' and rt['backLang'] == 'python' and 'def f' in (rt['backStarter'] or ''), rt)

    # ---------- ۵) رندر پاسخ دانش‌آموز در تصحیح ----------
    grading = page.evaluate("""async () => {
      const answer = window.SiteCode.fence('console.log("جواب من");', 'javascript');
      const h = await window.SiteStudent.richHtml(answer);
      const d = document.createElement('div'); d.innerHTML = h;
      return {hasBlock: !!d.querySelector('.cx-block'), badge: (d.querySelector('.cx-badge')||{}).textContent,
              text: (d.querySelector('.cx-code')||{}).textContent};
    }""")
    check('پاسخ کد در مسیر تصحیح به بلوک رنگی تبدیل می‌شود', grading['hasBlock'], grading)
    check('زبان پاسخ درست تشخیص داده می‌شود', grading['badge'] == 'JS', grading['badge'])
    check('متن کد دست‌نخورده می‌ماند', 'جواب من' in (grading['text'] or ''), grading['text'])

    # ---------- ۶) پاسخ‌دادن واقعی دانش‌آموز روی موبایل ----------
    EXAM_CODE = 'CODE48'
    exam_id = 'exam-v348-code'
    mock_state.exams[exam_id] = {
        'id': exam_id, 'code': EXAM_CODE, 'title': 'آزمون برنامه‌نویسی', 'subject': 'کامپیوتر',
        'duration': 30, 'total_score': 20, 'is_open': True,
        'teacher_id': next(iter(mock_state.users_by_id), ''),
        'questions': [
            {'type': 'essay', 'mode': 'code', 'code_lang': 'python',
             'code_starter': 'def solve(n):\n    pass', 'text': 'تابع را کامل کن.', 'score': 10},
            {'type': 'essay', 'mode': 'code', 'text': 'به زبان دلخواه بنویس.', 'score': 10},
        ],
        'options': {}, 'shuffle_q': False, 'shuffle_opt': False, 'attempts_allowed': 1,
        'grade_policy': 'last', 'pick_count': 0, 'pick_rules': {}, 'audience': 'all',
        'class_id': None, 'created_at': mock_state.now_iso(),
    }
    mock_state.exam_keys[exam_id] = {'exam_id': exam_id, 'answers': [{}, {}]}

    mctx = browser.new_context(viewport={'width': 390, 'height': 844}, is_mobile=True, has_touch=True)
    mp = mctx.new_page()
    merrors = []
    mp.on('pageerror', lambda e: merrors.append(str(e)))
    mp.goto(web_url + '/', wait_until='domcontentloaded')
    e2e.click_visible_button(mp, 'ورود / ثبت', substring=True)
    mp.wait_for_selector("input[type='password']", timeout=60000)
    mp.locator("input:not([type='password']):not([type='hidden']):not([type='checkbox'])").first.fill('uitest_student')
    mp.locator("input[type='password']").first.fill('StudentPass123!')
    mp.locator("input[type='password']").first.press('Enter')
    mp.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)
    mp.evaluate("() => window.ExamSite.go('join')")
    cin = mp.locator("input[placeholder*='کد']").first
    cin.wait_for(timeout=30000)
    cin.fill(EXAM_CODE)
    e2e.click_visible_button(mp, 'ورود به آزمون', substring=True)
    mp.wait_for_selector('.cx-answer', timeout=60000)
    check('دانش‌آموز برای سؤال کد، بخش «پاسخ کدنویسی» می‌بیند', True)
    hint = mp.eval_on_selector('.cx-answer .st-hint', 'e => e.textContent')
    check('زبانِ ثابت‌شدهٔ معلم به دانش‌آموز گفته می‌شود', 'پایتون' in hint, hint)

    mp.evaluate("() => Array.from(document.querySelectorAll('.cx-answer button')).find(b => /نوشتن کد/.test(b.textContent)).click()")
    mp.wait_for_selector('.cx-editor .cx-ta', timeout=20000)
    check('با زبان ثابت، محیط بی‌درنگ باز می‌شود (بدون انتخاب زبان)', mp.locator('.cx-grid').count() == 0)
    starter = mp.eval_on_selector('.cx-editor .cx-ta', 'e => e.value')
    check('کد آغازین معلم در ویرایشگر دانش‌آموز نشسته', 'def solve(n):' in starter, repr(starter))

    mta = mp.locator('.cx-editor .cx-ta')
    mta.fill('def solve(n):\n    return n * 2')
    mp.wait_for_timeout(250)
    overflow = mp.evaluate("() => document.documentElement.scrollWidth - window.innerWidth")
    check('محیط کد از عرض گوشی بیرون نمی‌زند', overflow <= 1, 'overflow=%dpx' % overflow)
    bar_rows = mp.evaluate("""() => {
      const bar = document.querySelector('.cx-editor .cx-bar');
      const kids = Array.from(bar.children).filter(c => c.offsetParent !== null);
      const centers = kids.map(c => { const r = c.getBoundingClientRect(); return Math.round(r.top + r.height / 2); });
      const wrapped = kids.some((c, i) => i > 0 && c.getBoundingClientRect().top >= kids[i - 1].getBoundingClientRect().bottom - 1);
      return {wrapped: wrapped, height: bar.offsetHeight, centers: centers};
    }""")
    check('نوار محیط کد در گوشی یک‌خطی می‌ماند', not bar_rows['wrapped'], bar_rows)
    mp.screenshot(path=str(OUT / 'student-mobile-editor.png'))
    mp.evaluate("() => Array.from(document.querySelectorAll('button')).find(b => /ذخیره در پاسخ/.test(b.textContent)).click()")
    mp.wait_for_selector('.cx-answer .cx-block', timeout=20000)
    saved = mp.eval_on_selector('.cx-answer .cx-block .cx-code', 'e => e.textContent')
    check('پاسخ دانش‌آموز به‌صورت بلوک رنگی نشان داده می‌شود', 'return n * 2' in saved, repr(saved))
    draft = mp.evaluate("() => Object.keys(localStorage).map(k => localStorage.getItem(k) || '').join('\\n')")
    check('پاسخ به‌شکل بلوک کد ذخیره می‌شود', '```python' in draft and 'return n * 2' in draft)
    check('دکمهٔ ویرایش پاسخ پیدا می‌شود', mp.locator('.cx-answer button', has_text='ویرایش کد').count() == 1)
    mp.screenshot(path=str(OUT / 'student-mobile-answer.png'))

    # سؤال دوم: زبان آزاد ⇒ باید فهرست زبان باز شود
    mp.evaluate("() => { const b = Array.from(document.querySelectorAll('button')).find(x => /بعدی|سؤال ۲|^۲$/.test(x.textContent.trim())); if (b) b.click(); }")
    mp.wait_for_timeout(700)
    free = mp.evaluate("() => Array.from(document.querySelectorAll('.cx-answer button')).filter(b => /نوشتن کد/.test(b.textContent)).length")
    if free:
        mp.evaluate("() => Array.from(document.querySelectorAll('.cx-answer button')).find(b => /نوشتن کد/.test(b.textContent)).click()")
        mp.wait_for_selector('.cx-grid', timeout=20000)
        tiles = mp.eval_on_selector_all('.cx-grid .cx-tile', 'e => e.length')
        check('برای زبان آزاد، دانش‌آموز خودش از ۱۲ زبان انتخاب می‌کند', tiles == 12, tiles)
        mp.screenshot(path=str(OUT / 'student-mobile-picker.png'))
    else:
        check('سؤال دوم (زبان آزاد) در دسترس است', False, 'دکمهٔ نوشتن کد پیدا نشد')
    check('در گوشی هیچ خطای جاوااسکریپتی رخ نداد', not merrors, merrors[:3])
    mctx.close()

    check('هیچ خطای جاوااسکریپتی رخ نداد', not errors, errors[:3])
    browser.close()

print()
for f in fails:
    print('  x', f)
print('تصویرها:', OUT)
print('CODE_TOOL_CHECK:', 'FAIL (%d)' % len(fails) if fails else 'PASS')
sys.exit(1 if fails else 0)
