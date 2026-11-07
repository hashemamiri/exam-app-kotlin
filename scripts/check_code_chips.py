#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V350 — کد به‌عنوان شیء داخل متن سؤال و گزینه‌ها (ابزار محلی؛ در CI اجرا نمی‌شود).
اجرا:  python3 scripts/check_code_chips.py
تصویرها در $V350_OUT (پیش‌فرض /tmp/v350-check).

می‌سنجد: کد در همهٔ گونه‌های سؤال و در گزینه‌ها تراشه می‌شود، یک کلیک انتخاب و
ضربدر حذف، دو کلیک ویرایش، هم‌زیستی با فرمول/شکل، و ذخیرهٔ درست در متن."""
import os
import pathlib
import sys
import tempfile

SCRIPTS = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import mock_supabase_server as mss           # noqa: E402
import web_ui_e2e as e2e                     # noqa: E402
from playwright.sync_api import sync_playwright  # noqa: E402

OUT = pathlib.Path(os.environ.get('V350_OUT', '/tmp/v350-check'))
OUT.mkdir(parents=True, exist_ok=True)
fails = []

QT = '.b-editor .b-ta-wrap:not(.sm) textarea'
QR = '.b-editor .b-ta-wrap:not(.sm) .b-rich'
OT = '.b-opt .b-ta-wrap textarea'
OR = '.b-opt .b-ta-wrap .b-rich'
SAVE = '.cx-editor .cx-foot .btn:not(.light)'


def check(name, cond, extra=''):
    print(('  OK   ' if cond else '  FAIL ') + name + (('  → ' + str(extra)) if extra else ''))
    if not cond:
        fails.append(name)


def login(page, url, mobile=False):
    page.goto(url + '/', wait_until='domcontentloaded')
    if mobile:
        # روی گوشی اول باید دکمهٔ ورود لمس شود
        e2e.click_visible_button(page, 'ورود / ثبت', substring=True)
        page.wait_for_selector("input[type='password']", timeout=60000)
    user_sel = ("input:not([type='password']):not([type='hidden']):not([type='checkbox'])"
                if mobile else "input[type='text']")
    page.locator(user_sel).first.fill('uitest_teacher')
    page.locator("input[type='password']").first.fill('TeacherPass123!')
    page.locator("input[type='password']").first.press('Enter')
    page.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)
    page.wait_for_function("() => window.ExamSite && window.ExamSite.user()", timeout=30000)
    page.evaluate("() => window.ExamSite.loadPanels()")
    page.wait_for_function("() => window.SiteCode && window.SiteBuilder", timeout=60000)


def pick_type(page, label, nav=False):
    # ناوبری فقط یک‌بار: go('builder') بار دوم به فهرست آزمون‌ها می‌رود
    if nav:
        page.evaluate("() => window.ExamSite.go('builder')")
        page.wait_for_selector('.b-add button', state='attached', timeout=60000)
    page.evaluate(
        """(t) => Array.from(document.querySelectorAll('.b-add button'))
             .find(b => b.textContent.trim().indexOf(t) >= 0).click()""", label)
    page.wait_for_selector('.b-tools .tool-btn.is-code', timeout=60000)


def put_code(page, tool_sel, code, lang_tile='پایتون'):
    """ابزار کد را می‌زند، زبان را برمی‌گزیند، کد را می‌نویسد و ذخیره می‌کند."""
    page.click(tool_sel)
    page.wait_for_selector('.cx-grid .cx-tile', timeout=20000)
    page.click('.cx-grid .cx-tile:has-text("%s")' % lang_tile)
    page.wait_for_selector('.cx-editor .cx-ta', timeout=20000)
    page.fill('.cx-editor .cx-ta', code)
    page.click(SAVE)
    page.wait_for_selector('.cx-editor', state='detached', timeout=20000)


mock_srv, mock_state = mss.start_server_in_thread('127.0.0.1', 0)
mock_url = 'http://127.0.0.1:%d' % mock_srv.server_address[1]
anon = mss.make_public_anon_key()
tmp = tempfile.TemporaryDirectory(prefix='v350_site_')
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
    login(page, web_url)

    # ---------- ۱) گونهٔ غیرتشریحی: کد باید در دسترس باشد ----------
    print('۱) کد در همهٔ گونه‌های سؤال')
    for i, label in enumerate(['چندگزینه‌ای', 'صحیح / غلط', 'جای‌خالی', 'عددی', 'جورکردنی', 'تشریحی']):
        pick_type(page, label, nav=(i == 0))
        has = page.locator('.b-tools .tool-btn.is-code').count() == 1
        check('ابزار کد برای «' + label + '» هست', has)

    # ---------- ۲) متن سؤال: تراشهٔ کد ----------
    print('\n۲) کد داخل متن سؤال (چندگزینه‌ای)')
    pick_type(page, 'چندگزینه‌ای')
    # textarea پشت ویرایشگر تراشه‌ای پنهان است؛ مقدار از همان راه خودش ست می‌شود
    page.evaluate("""() => {
      const ta = document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea');
      ta.value = 'این کد چه چاپ می‌کند؟';
      ta.dispatchEvent(new Event('input'));
      ta.selectionStart = ta.selectionEnd = ta.value.length;
    }""")
    put_code(page, '.b-tools .tool-btn.is-code', 'print(2 + 3)')
    page.wait_for_selector(QR + ' .b-chip.code', timeout=20000)
    check('کد داخل متن سؤال تراشه شد', True)
    live = page.locator(QR + ' .b-chip.code.code-live').count() == 1
    check('روی دسکتاپ خودِ کادر کد دیده می‌شود (نه کد خام)', live)
    raw = page.eval_on_selector(QT, 'e => e.value')
    check('زیرِ تراشه، همان قالب ذخیره‌سازی نشسته',
          raw.startswith('این کد چه چاپ می‌کند؟') and '```python' in raw and 'print(2 + 3)' in raw,
          raw.replace('\n', '\\n')[:90])
    shown = page.eval_on_selector(QR + ' .b-chip.code', 'e => e.textContent')
    check('کد در تراشه رنگی و بی‌بک‌تیک است', 'print' in shown and '```' not in shown)
    check('شمارهٔ خط در تراشه هست',
          page.locator(QR + ' .b-chip.code .cx-num').count() == 1)
    page.screenshot(path=str(OUT / 'chip-in-question.png'))

    # ---------- ۳) یک کلیک: انتخاب + ضربدر حذف ----------
    print('\n۳) یک کلیک ⇒ انتخاب و امکان حذف')
    page.click(QR + ' .b-chip.code')
    page.wait_for_selector(QR + ' .b-chip.code.sel', timeout=10000)
    check('یک کلیک، تراشه را انتخاب می‌کند', True)
    check('ضربدر حذف پیدا می‌شود',
          page.locator(QR + ' .b-chip.code .b-chip-x').count() == 1)

    # ---------- ۴) دو کلیک: ویرایش ----------
    print('\n۴) دو کلیک ⇒ ویرایش')
    page.dblclick(QR + ' .b-chip.code')
    page.wait_for_selector('.cx-editor .cx-ta', timeout=20000)
    got = page.input_value('.cx-editor .cx-ta')
    check('ویرایشگر با همان کد باز می‌شود', got.strip() == 'print(2 + 3)', got)
    picked = page.eval_on_selector('.cx-editor .cx-bar', 'e => e.textContent') \
        if page.locator('.cx-editor .cx-bar').count() else ''
    check('زبانِ ذخیره‌شده حفظ می‌شود', 'پایتون' in picked or picked == '', picked)
    page.fill('.cx-editor .cx-ta', 'print(7 * 6)')
    page.click(SAVE)
    page.wait_for_selector('.cx-editor', state='detached', timeout=20000)
    page.wait_for_function(
        "() => document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea').value.indexOf('7 * 6') >= 0",
        timeout=20000)
    raw2 = page.eval_on_selector(QT, 'e => e.value')
    check('ویرایش در همان جای متن نشست', 'print(7 * 6)' in raw2 and 'print(2 + 3)' not in raw2)
    check('تراشه دوباره ساخته شد (یکی، نه دوتا)',
          page.locator(QR + ' .b-chip.code').count() == 1)
    check('متن اطراف کد دست‌نخورده ماند', raw2.startswith('این کد چه چاپ می‌کند؟'))

    # ---------- ۵) حذف با ضربدر ----------
    print('\n۵) حذف با یک کلیک روی ضربدر')
    page.click(QR + ' .b-chip.code')
    page.click(QR + ' .b-chip.code .b-chip-x')
    page.wait_for_function(
        "() => document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea').value.indexOf('```') < 0",
        timeout=20000)
    check('کد از متن پاک شد',
          page.locator(QR + ' .b-chip.code').count() == 0)
    check('متن سؤال باقی ماند',
          page.eval_on_selector(QT, 'e => e.value')
          .startswith('این کد چه چاپ می‌کند؟'))

    # ---------- ۶) گزینه‌ها ----------
    print('\n۶) کد داخل گزینه‌ها')
    page.locator('.b-opt .b-opt-plus').first.click()
    page.wait_for_selector('.b-ins-menu', timeout=15000)
    check('منوی درج گزینه، «کد» دارد',
          page.locator('.b-ins-menu .tool-btn.is-code').count() == 1)
    page.screenshot(path=str(OUT / 'option-insert-menu.png'))
    page.click('.b-ins-menu .tool-btn.is-code')
    page.wait_for_selector('.cx-grid .cx-tile', timeout=20000)
    page.click('.cx-grid .cx-tile:has-text("پایتون")')
    page.wait_for_selector('.cx-editor .cx-ta', timeout=20000)
    page.fill('.cx-editor .cx-ta', 'x = 5')
    page.click(SAVE)
    page.wait_for_selector('.cx-editor', state='detached', timeout=20000)
    page.wait_for_selector(OR + ' .b-chip.code', timeout=20000)
    check('کد داخل گزینه تراشه شد', True)
    oraw = page.eval_on_selector(OT, 'e => e.value')
    check('متن گزینه همان قالب را دارد', '```python' in oraw and 'x = 5' in oraw,
          oraw.replace('\n', '\\n'))
    page.click(OR + ' .b-chip.code')
    check('یک کلیک روی گزینه، ضربدر حذف می‌دهد',
          page.locator(OR + ' .b-chip.code .b-chip-x').count() == 1)
    page.dblclick(OR + ' .b-chip.code')
    page.wait_for_selector('.cx-editor .cx-ta', timeout=20000)
    check('دو کلیک روی گزینه، ویرایشگر را باز می‌کند',
          page.input_value('.cx-editor .cx-ta').strip() == 'x = 5')
    page.fill('.cx-editor .cx-ta', 'x = 9')
    page.click(SAVE)
    page.wait_for_selector('.cx-editor', state='detached', timeout=20000)
    page.wait_for_function(
        "() => document.querySelector('.b-opt .b-ta-wrap textarea').value.indexOf('x = 9') >= 0", timeout=20000)
    check('ویرایش گزینه کار می‌کند', True)
    page.click(OR + ' .b-chip.code')
    page.click(OR + ' .b-chip.code .b-chip-x')
    page.wait_for_function(
        "() => document.querySelector('.b-opt .b-ta-wrap textarea').value.indexOf('```') < 0", timeout=20000)
    check('حذف کد از گزینه کار می‌کند',
          page.locator(OR + ' .b-chip.code').count() == 0)

    # ---------- ۷) هم‌زیستی با فرمول و شکل ----------
    print('\n۷) کد در کنار فرمول و شکل')
    page.evaluate("""() => {
      const ta = document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea');
      ta.value = 'قبل $x^2$ وسط\\n```python\\nprint(1)\\n```\\nبعد $y_1$ پایان';
      ta.dispatchEvent(new Event('input'));
    }""")
    page.wait_for_function(
        "() => document.querySelectorAll('.b-editor .b-ta-wrap:not(.sm) .b-rich .b-chip').length === 3", timeout=20000)
    kinds = page.eval_on_selector_all(
        QR + ' .b-chip',
        "es => es.map(e => e.classList.contains('code') ? 'code' : e.classList.contains('tex') ? 'tex' : 'fig')")
    check('دو فرمول و یک کد، هر سه تراشه شدند', kinds == ['tex', 'code', 'tex'], kinds)
    kept = page.eval_on_selector(QT, 'e => e.value')
    check('هیچ‌کدام دیگری را نخورد',
          '$x^2$' in kept and '$y_1$' in kept and '```python' in kept)
    page.evaluate("""() => {
      const ta = document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea');
      ta.value = '```python\\ns = "$x$";\\n```\\nبعد';
      ta.dispatchEvent(new Event('input'));
    }""")
    page.wait_for_function(
        "() => document.querySelectorAll('.b-editor .b-ta-wrap:not(.sm) .b-rich .b-chip').length >= 1", timeout=20000)
    one = page.eval_on_selector_all(
        QR + ' .b-chip',
        "es => es.map(e => e.classList.contains('code') ? 'code' : 'tex')")
    check('علامت دلار داخل کد، تراشهٔ فرمول نمی‌سازد', one == ['code'], one)
    page.screenshot(path=str(OUT / 'code-with-formula.png'))

    # ---------- ۸) گوشی ----------
    print('\n۸) گوشی: تراشهٔ کد و حذف')
    mctx = browser.new_context(viewport={'width': 390, 'height': 844},
                              is_mobile=True, has_touch=True)
    mp = mctx.new_page()
    mp.on('pageerror', lambda e: errors.append('mobile: ' + str(e)))
    login(mp, web_url, mobile=True)
    pick_type(mp, 'چندگزینه‌ای', nav=True)
    mp.evaluate("""() => {
      const ta = document.querySelector('.b-editor .b-ta-wrap:not(.sm) textarea');
      ta.value = 'سؤال\\n```python\\nprint(1)\\n```';
      ta.dispatchEvent(new Event('input'));
    }""")
    mp.wait_for_selector(QR + ' .b-chip.code', timeout=20000)
    mlabel = mp.eval_on_selector(QR + ' .b-chip.code', 'e => e.textContent')
    check('روی گوشی برچسب کوتاه زبان دیده می‌شود', 'پایتون' in mlabel, mlabel)
    mp.click(QR + ' .b-chip.code')
    check('روی گوشی هم ضربدر حذف هست',
          mp.locator(QR + ' .b-chip.code .b-chip-x').count() == 1)
    mp.screenshot(path=str(OUT / 'mobile-chip.png'))
    mctx.close()

    check('هیچ خطای جاوااسکریپتی رخ نداد', not errors, errors[:3])
    browser.close()

web_srv.shutdown()
mock_srv.shutdown()
tmp.cleanup()
print('\nتصویرها: %s' % OUT)
print('CODE_CHIPS: ' + ('PASS' if not fails else 'FAIL ' + str(fails)))
sys.exit(1 if fails else 0)
