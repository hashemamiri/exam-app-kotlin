#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V344 — بررسی واقعی ریل کناری مدیر روی Chromium (ابزار محلی؛ در CI اجرا نمی‌شود).
اجرا:  python3 scripts/check_manager_rail.py
نیازمند playwright + chromium که در این محیط از پیش نصب‌اند.

تقویم حذف، کارت‌ها زیر کیف پول، صفحهٔ کارت‌ها سه کارت مدیر را نشان دهد،
و تقویم همچنان از «منو» باز شود."""
import os
import pathlib
import sys
import tempfile

SCRIPTS = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import mock_supabase_server as mss           # noqa: E402
import web_ui_e2e as e2e                     # noqa: E402
from playwright.sync_api import sync_playwright  # noqa: E402

OUT = pathlib.Path(os.environ.get('V344_OUT', '/tmp/v344-check'))
OUT.mkdir(parents=True, exist_ok=True)
fails = []


def check(name, cond, extra=''):
    print(('  OK   ' if cond else '  FAIL ') + name + (('  → ' + str(extra)) if extra else ''))
    if not cond:
        fails.append(name)


mock_srv, _ = mss.start_server_in_thread('127.0.0.1', 0)
mock_url = 'http://127.0.0.1:%d' % mock_srv.server_address[1]
anon = mss.make_public_anon_key()
tmp = tempfile.TemporaryDirectory(prefix='v344_site_')
site_dir = pathlib.Path(tmp.name)
e2e.build_chunked_site_out(site_dir, anon)
web_srv, web_url = e2e.start_readonly_site_server(site_dir, mock_url, anon)
print('site:', web_url, '\n')

with sync_playwright() as pw:
    browser = pw.chromium.launch()
    ctx = browser.new_context(viewport={'width': 1366, 'height': 900})
    page = ctx.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.goto(web_url + '/', wait_until='domcontentloaded')
    page.locator("input[type='text']").first.fill('uitest_manager')
    page.locator("input[type='password']").first.fill('ManagerPass123!')
    page.locator("input[type='password']").first.press('Enter')
    page.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)
    page.wait_for_selector('.dk-rail', timeout=30000)

    labels = page.eval_on_selector_all('.dk-rail .dk-rail-item',
                                       'els => els.map(e => e.getAttribute("aria-label"))')
    print('ریل مدیر:', ' | '.join(labels), '\n')
    check('«تقویم» در ریل نیست', 'تقویم' not in labels)
    check('«کارت‌ها» در ریل هست', 'کارت‌ها' in labels)
    check('«کارت‌ها» بلافاصله زیر «کیف پول» است',
          labels.index('کارت‌ها') == labels.index('کیف پول') + 1,
          '%s → %s' % (labels[labels.index('کیف پول')], labels[labels.index('کارت‌ها')]))
    check('ترتیب بقیه دست‌نخورده', labels[:6] == ['منو', 'داشبورد', 'معلم‌ها', 'مدرسه', 'کلاس‌ها', 'دانش‌آموزان'], labels[:6])
    page.screenshot(path=str(OUT / 'manager-rail.png'))

    # صفحهٔ کارت‌ها
    page.click('.dk-rail .dk-rail-item[aria-label="کارت‌ها"]')
    page.wait_for_selector('.dk-cards .dk-gcard', timeout=30000)
    titles = page.eval_on_selector_all('.dk-cards .dk-gcard b', 'els => els.map(e => e.textContent)')
    check('سه کارت مدیر نمایش داده می‌شود', titles == ['مدارس', 'کارنامه', 'وضعیت'], titles)
    small = page.eval_on_selector_all('.dk-cards .dk-gcard small', 'els => els.map(e => e.textContent)')
    check('برچسب کارت‌ها «مدرسه» است نه «آزمون آنلاین»', set(small) == {'مدرسه'}, small)
    active = page.eval_on_selector_all('.dk-rail .dk-rail-item.active',
                                       'els => els.map(e => e.getAttribute("aria-label"))')
    check('دکمهٔ کارت‌ها در ریل روشن می‌شود', active == ['کارت‌ها'], active)
    h1 = page.eval_on_selector('.dk-title h1', 'e => e.textContent')
    check('عنوان صفحه «کارت‌ها» است', h1 == 'کارت‌ها', h1)
    page.screenshot(path=str(OUT / 'manager-cards.png'))

    # تقویم همچنان از منو در دسترس است
    page.click('.dk-rail .dk-rail-item[aria-label="منو"]')
    page.wait_for_selector('.dk-grid', timeout=30000)
    page.wait_for_function("() => document.querySelector('#content') && document.querySelector('#content').textContent.indexOf('تقویم') >= 0", timeout=15000)
    menu_txt = page.eval_on_selector('#content', 'e => e.textContent')
    check('«تقویم» در صفحهٔ منو باقی است', 'تقویم' in menu_txt)
    cal_btn = page.locator('#content button', has_text='تقویم').first
    check('کارت تقویم در منو کلیک‌پذیر است', cal_btn.count() > 0)
    cal_btn.click()
    page.wait_for_function("() => document.querySelector('.dk-title h1') && document.querySelector('.dk-title h1').textContent.indexOf('تقویم') >= 0", timeout=20000)
    check('تقویم از منو باز می‌شود', True, page.eval_on_selector('.dk-title h1', 'e => e.textContent'))
    page.screenshot(path=str(OUT / 'manager-menu.png'))

    check('هیچ خطای جاوااسکریپتی نبود', not errors, errors[:2])
    browser.close()

tmp.cleanup()
print()
for f in fails:
    print('  x', f)
print('V344_BROWSER_CHECK:', 'FAIL (%d)' % len(fails) if fails else 'PASS')
sys.exit(1 if fails else 0)
