#!/usr/bin/env python3
"""V233 — اسکن کد مرده (بدون کامپایلر): فایل‌های کاتلین اپ که هیچ‌کدام از نام‌های سطح بالای آن‌ها
(class/object/interface/fun/val/typealias) در هیچ فایل دیگری (main/test/site/scripts) نیامده باشد،
و توابع سطح بالای JS سایت که فقط یک بار (تعریف) دیده می‌شوند.
خروجی فقط گزارش است؛ حذف با بررسی انسانی. اجرا از ریشهٔ مخزن: python3 scripts/dead_code_scan.py
"""
import re, subprocess, sys

def tracked(prefixes):
    out = subprocess.check_output(['git', 'ls-files'] + prefixes, text=True).split()
    return [f for f in out if f.endswith(('.kt', '.xml', '.js', '.html', '.py'))]

def main():
    files = tracked(['app/src', 'site/src', 'scripts'])
    texts = {}
    for f in files:
        try: texts[f] = open(f, encoding='utf-8').read()
        except Exception: pass
    idents = {f: set(re.findall(r'[A-Za-z_]\w*', t)) for f, t in texts.items()}
    decl = re.compile(r'^(?:@\w+(?:\([^)]*\))?\s+)*(?:public |internal |private )?(?:data |sealed |abstract |open |enum |annotation |value |inline |suspend |operator |infix |const |lateinit )*(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s+)?(?:[\w.]+\.)?(\w+)', re.M)
    dead_kt = []
    for f in files:
        if not (f.startswith('app/src/main/java') and f.endswith('.kt')): continue
        names = set(decl.findall(texts.get(f, ''))) - {'invoke', 'main'}
        if names and not any(n in idents[g] for g in idents if g != f for n in names): dead_kt.append(f)
    from collections import Counter
    count = Counter()
    for t in texts.values(): count.update(re.findall(r'[A-Za-z_]\w*', t))
    dead_js = []
    for f, t in texts.items():
        if not (f.startswith('site/src') and f.endswith('.js')): continue
        for m in re.finditer(r'^\s*(?:async\s+)?function\s+(\w+)\s*\(', t, re.M):
            if count[m.group(1)] <= 1: dead_js.append(f + ':' + m.group(1))
    for x in dead_kt: print('کاتلین بی‌استفاده:', x)
    for x in dead_js: print('تابع JS بی‌استفاده:', x)
    print('dead_code_scan:', len(dead_kt), 'فایل کاتلین،', len(dead_js), 'تابع JS')
    return 0

if __name__ == '__main__':
    sys.exit(main())
