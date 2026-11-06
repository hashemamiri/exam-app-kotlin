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
    check('چهار کارت مدیر مثل اپ', titles == ['مدارس', 'کارنامه', 'کارنامه (PDF)', 'وضعیت'], titles)
    small = page.eval_on_selector_all('.dk-cards .dk-gcard small', 'els => els.map(e => e.textContent)')
    check('برچسب کارت‌ها «مدرسه» است نه «آزمون آنلاین»', set(small) == {'مدرسه'}, small)
    active = page.eval_on_selector_all('.dk-rail .dk-rail-item.active',
                                       'els => els.map(e => e.getAttribute("aria-label"))')
    check('دکمهٔ کارت‌ها در ریل روشن می‌شود', active == ['کارت‌ها'], active)
    h1 = page.eval_on_selector('.dk-title h1', 'e => e.textContent')
    check('عنوان صفحه «کارت‌ها» است', h1 == 'کارت‌ها', h1)
    page.screenshot(path=str(OUT / 'manager-cards.png'))

    # V344.1 — کارنامه و وضعیت باید دو نمای متفاوت باز کنند
    def open_card(label):
        page.click('.dk-rail .dk-rail-item[aria-label="کارت‌ها"]')
        page.wait_for_selector('.dk-cards .dk-gcard', timeout=30000)
        page.locator('.dk-cards .dk-gcard', has_text=label).first.click()

    open_card('کارنامه')
    page.wait_for_function("() => document.querySelector('.mgr-dash-head h2')", timeout=30000)
    h2 = page.eval_on_selector('.mgr-dash-head h2', 'e => e.textContent')
    check('کارت «کارنامه» نمای کارنامهٔ مدرسه را باز می‌کند', h2 == 'کارنامه مدرسه', h2)
    on_tab = page.eval_on_selector_all('.mgr-dash-tabs button.on', 'els => els.map(e => e.textContent)')
    check('تب «کارنامه مدرسه» روشن است', on_tab == ['کارنامه مدرسه'], on_tab)

    open_card('وضعیت')
    page.wait_for_function("() => document.querySelector('.mgr-dash-head h2')", timeout=30000)
    h2b = page.eval_on_selector('.mgr-dash-head h2', 'e => e.textContent')
    check('کارت «وضعیت» نمای کلی داشبورد را باز می‌کند', h2b == 'داشبورد مدیر', h2b)
    check('کارنامه و وضعیت دیگر یک صفحه نیستند', h2 != h2b, '%s ≠ %s' % (h2, h2b))

    # V344.1 — کارت کارنامه (PDF)
    open_card('کارنامه (PDF)')
    page.wait_for_selector('.mgr-rc .dk-gcard', timeout=30000)
    kinds = page.eval_on_selector_all('.mgr-rc .dk-gcard b', 'els => els.map(e => e.textContent)')
    check('سه گونهٔ کارنامه مثل اپ', kinds == ['کارنامهٔ کلاس', 'کارنامهٔ دانش‌آموز', 'کارنامهٔ مدرسه (دفترچه)'], kinds)
    rc_active = page.eval_on_selector_all('.dk-rail .dk-rail-item.active', 'els => els.map(e => e.getAttribute("aria-label"))')
    check('در صفحهٔ PDF هم دکمهٔ کارت‌ها روشن می‌ماند', rc_active == ['کارت‌ها'], rc_active)
    rc_title = page.eval_on_selector('.dk-title h1', 'e => e.textContent')
    check('عنوان صفحه «کارنامه (PDF)» است', rc_title == 'کارنامه (PDF)', rc_title)
    page.screenshot(path=str(OUT / 'manager-reportcards.png'))

    page.locator('.mgr-rc .dk-gcard', has_text='کارنامهٔ کلاس').first.click()
    page.wait_for_selector('.mgr-rc .card button', timeout=30000)
    n_classes = len(page.query_selector_all('.mgr-rc .card button'))
    check('فهرست کلاس‌ها می‌آید', n_classes > 0, '%d کلاس' % n_classes)
    page.locator('.mgr-rc .card button', has_text='ساخت کارنامهٔ کلاس').first.click()
    page.wait_for_function("() => document.querySelector('.mgr-rc table') || document.querySelector('.mgr-rc .empty')", timeout=40000)
    has_pdf = page.locator('.mgr-rc button', has_text='ذخیرهٔ PDF').count() > 0
    check('جدول کارنامهٔ کلاس + دکمهٔ PDF', has_pdf)
    page.screenshot(path=str(OUT / 'manager-class-report.png'))

    # انتخاب قالب: هر ۱۰ قالب باید دیده شوند
    page.locator('.mgr-rc button', has_text='ذخیرهٔ PDF').first.click()
    fr = page.frame_locator("iframe[title='report-card-templates']")
    fr.locator('.rc-picker .rc-card').first.wait_for(timeout=40000)
    n_tpl = len(fr.locator('.rc-picker .rc-card').all())
    check('انتخابگر قالب با ۱۰ قالب باز می‌شود', n_tpl == 10, '%d قالب' % n_tpl)
    page.screenshot(path=str(OUT / 'manager-template-picker.png'))
    page.evaluate("() => window.ExamReportCard.close()")
    page.wait_for_timeout(300)

    # مسیر دانش‌آموز
    page.click('.dk-rail .dk-rail-item[aria-label="کارت‌ها"]')
    page.wait_for_selector('.dk-cards .dk-gcard', timeout=30000)
    open_card('کارنامه (PDF)')
    page.wait_for_selector('.mgr-rc .dk-gcard', timeout=30000)
    page.locator('.mgr-rc .dk-gcard', has_text='کارنامهٔ دانش‌آموز').first.click()
    page.wait_for_selector('.mgr-rc .card button', timeout=30000)
    page.locator('.mgr-rc .card button', has_text='انتخاب دانش‌آموز').first.click()
    page.wait_for_selector('.mgr-rc .card button', timeout=40000)
    n_st = len(page.locator('.mgr-rc .card button', has_text='ساخت کارنامه').all())
    check('فهرست دانش‌آموزان کلاس می‌آید', n_st > 0, '%d دانش‌آموز' % n_st)

    # دفترچهٔ مدرسه
    open_card('کارنامه (PDF)')
    page.wait_for_selector('.mgr-rc .dk-gcard', timeout=30000)
    page.locator('.mgr-rc .dk-gcard', has_text='دفترچه').first.click()
    fr2 = page.frame_locator("iframe[title='report-card-templates']")
    fr2.locator('.rc-picker .rc-card').first.wait_for(timeout=90000)
    n_tpl2 = len(fr2.locator('.rc-picker .rc-card').all())
    check('دفترچهٔ مدرسه هم به انتخابگر ۱۰ قالب می‌رسد', n_tpl2 == 10, '%d قالب' % n_tpl2)
    page.evaluate("() => window.ExamReportCard.close()")
    page.wait_for_timeout(300)

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
