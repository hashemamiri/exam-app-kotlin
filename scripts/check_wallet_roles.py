#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V341 — نگهبان نقش‌های کیف پول.

چرا: تابع.native_create_wallet_payment_order در V38 برای «معلم و مدیر» باز شد، ولی
V335.32 (افزودن زیبال) همان تابع را از الگوی قدیمیِ «فقط معلم» بازسازی کرد و نقش مدیر
ناخواسته حذف شد؛ مدیر هنگام شارژ پیام «حساب معلم معتبر نیست» می‌گرفت.

این نگهبان آخرین تعریف تابع در مهاجرت‌ها (بر اساس ترتیب نام فایل) و همهٔ فایل‌های دستی
کیف پول را می‌سنجد: نقش manager باید مجاز باشد و درگاه zibal از قلم نیفتاده باشد.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
FN = 'native_create_wallet_payment_order'
SIG = re.compile(r'create\s+or\s+replace\s+function\s+public\.' + FN + r'\b', re.I)


def body_of(text, start):
    """بدنهٔ تابع تا پایان بلوک dollar-quoted."""
    m = re.search(r'\$\$', text[start:])
    if not m:
        return text[start:start + 4000]
    open_at = start + m.end()
    close = text.find('$$', open_at)
    return text[start:close if close != -1 else len(text)]


def check(path):
    text = path.read_text(encoding='utf-8')
    problems = []
    for m in SIG.finditer(text):
        body = body_of(text, m.start())
        role_ok = re.search(r"role\s+in\s*\(\s*'teacher'\s*,\s*'manager'\s*\)", body) is not None
        teacher_only = re.search(r"role\s*=\s*'teacher'", body) is not None
        if teacher_only or not role_ok:
            problems.append('نقش manager مجاز نیست (بعد از V38 باید teacher+manager باشد)')
        if "'zibal'" not in body:
            problems.append('درگاه zibal در فهرست درگاه‌ها نیست')
        if 'to service_role' not in text[m.start():m.start() + len(body) + 600]:
            problems.append('grant اجرا فقط برای service_role نیست')
    return problems


def main():
    migrations = sorted((ROOT / 'supabase/migrations').glob('*.sql'))
    definers = [p for p in migrations if SIG.search(p.read_text(encoding='utf-8'))]
    fails = []
    if not definers:
        fails.append('هیچ مهاجرتی تابع %s را تعریف نمی‌کند' % FN)
    else:
        latest = definers[-1]
        problems = check(latest)
        print('آخرین تعریف در مهاجرت‌ها: %s' % latest.name)
        for pr in problems:
            fails.append('%s → %s' % (latest.name, pr))

    manual = sorted((ROOT / 'sql/manual').glob('SQL_WALLET_*.sql'))
    for p in manual:
        if not SIG.search(p.read_text(encoding='utf-8')):
            continue
        for pr in check(p):
            fails.append('%s → %s' % (p.name, pr))
        print('فایل دستی بررسی شد: %s' % p.name)

    for f in fails:
        print('  x', f)
    print('WALLET_ROLES:', 'FAIL (%d)' % len(fails) if fails else 'PASS')
    return 1 if fails else 0


if __name__ == '__main__':
    sys.exit(main())
