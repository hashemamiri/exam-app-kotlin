#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V193.1 — بررسی محلی پین‌های متنی تست‌ها (assertTrue("…" in source("…"))) بدون Gradle.
هر پینِ assertTrue که در فایل مبدأ نباشد گزارش می‌شود؛ خروجی غیرصفر = تست در CI می‌شکند.
فقط الگوهای ساده: val x = source("path") و "literal" in x / in source("path"). رشته‌های دارای $ (قالب کاتلین) نادیده گرفته می‌شوند."""
import re, glob, os, sys
def unescape(s):
    return re.sub(r'\\(.)', lambda m: {'n': '\n', 't': '\t', 'r': '\r'}.get(m.group(1), m.group(1)), s)
bad = 0
for t in sorted(glob.glob('app/src/test/java/ir/exam/app/**/*Test.kt', recursive=True)):
    src = open(t, encoding='utf-8').read()
    vars_ = {}
    for line in src.splitlines():
        for m in re.finditer(r'val (\w+) = source\("([^"]+)"\)', line): vars_[m.group(1)] = m.group(2)
        if 'assertTrue(' not in line or 'assertFalse(' in line: continue
        for m in re.finditer(r'"((?:[^"\\]|\\.)*)" in (\w+|source\("([^"]+)"\))', line):
            lit = m.group(1)
            if '$' in lit: continue
            path = m.group(3) or vars_.get(m.group(2))
            if not path or not os.path.exists(path): continue
            if unescape(lit) not in open(path, encoding='utf-8').read():
                bad += 1; print('PIN MISSING: %s -> %s : %s' % (os.path.basename(t), path, lit[:100]))
print('check_test_pins: %d missing' % bad)
sys.exit(1 if bad else 0)
