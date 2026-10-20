#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V193.1 — بررسی محلی پین‌های متنی تست‌ها بدون Gradle.
V220.1 — علاوه بر assertTrue("…" in x)، این الگوها هم بررسی می‌شوند (علت شکست CI در V218 نادیده‌گرفتن assertFalse بود):
  assertFalse("…" in x) · assertTrue(!x.contains("…")) · assertFalse(x.contains("…")) · assertTrue(x.contains("…"))
  assertTrue(Regex("…").findAll(x).count() == 0) / containsMatchIn
و با --strict (یا متغیر محیطی PINS_STRICT=1) هر خط assert که قابل تحلیل نیست در فایل‌های تست داده‌شده گزارش می‌شود
تا به‌صورت دستی بررسی شود:  python3 scripts/check_test_pins.py --strict app/src/test/.../V2xx_*.kt
خروجی غیرصفر = تست در CI می‌شکند."""
import re, glob, os, sys

def unescape(s):
    return re.sub(r'\\(.)', lambda m: {'n': '\n', 't': '\t', 'r': '\r'}.get(m.group(1), m.group(1)), s)

LIT = r'"((?:[^"\\]|\\.)*)"'
args = [a for a in sys.argv[1:] if not a.startswith('--')]
strict = '--strict' in sys.argv or os.environ.get('PINS_STRICT') == '1'
files = args or sorted(glob.glob('app/src/test/java/ir/exam/app/**/*Test.kt', recursive=True))
bad = 0; unparsed = 0; checked = 0; unresolved = 0

def read(path):
    try: return open(path, encoding='utf-8').read()
    except OSError: return None

for t in files:
    src = open(t, encoding='utf-8').read()
    vars_ = {}
    for line in src.splitlines():
        for m in re.finditer(r'val (\w+) = (?:source|src)\("([^"]+)"\)', line): vars_[m.group(1)] = m.group(2)
        # V220.2 — الگوهای دیگر خواندن فایل در تست‌های قدیمی (Neumorphic69IntegrationTest و …)
        for m in re.finditer(r'val (\w+) = File\(root(?:\(\))?, "([^"]+)"\)\.readText\(\)', line): vars_[m.group(1)] = m.group(2)
        for m in re.finditer(r'val (\w+) = root(?:\(\))?\.resolve\("([^"]+)"\)\.readText\(\)', line): vars_[m.group(1)] = m.group(2)
        s = line.strip()
        if not (s.startswith('assertTrue(') or s.startswith('assertFalse(')): continue
        neg_all = s.startswith('assertFalse(')
        found_any = False
        def check(lit, path, must_exist, where):
            global bad, checked
            if '$' in lit: return
            if not path: return
            body = read(path)
            if body is None: return
            checked += 1
            present = unescape(lit) in body
            if present != must_exist:
                bad += 1
                print('PIN %s: %s -> %s : %s' % ('MISSING' if must_exist else 'UNEXPECTED', os.path.basename(t), path, lit[:110]))
        def resolve(ref, inline_path):
            return inline_path or vars_.get(ref)
        # "lit" in x   /   "lit" in source("p")   — با ! قبل از پرانتز هم (مثل !( "lit" in x ))
        # V325 — شاخهٔ src("…") باید پیش از \w+ بیاید؛ وگرنه «src» به‌عنوان نام متغیر گرفته می‌شد
        # و پین بی‌صدا نادیده می‌ماند (علت شکست V232_SitePushTest در CI).
        for m in re.finditer(r'(!?)\(?' + LIT + r' in (?:(?:source|src)\("([^"]+)"\)|(\w+))', s):
            found_any = True
            negated = (m.group(1) == '!') ^ neg_all
            path = m.group(3) or vars_.get(m.group(4) or '')
            if not path:
                unresolved += 1
                if strict:
                    print('PIN UNRESOLVED (check by hand): %s : %s' % (os.path.basename(t), s[:140]))
                continue
            check(m.group(2), path, not negated, s)
        # x.contains("lit")  /  !x.contains("lit")
        for m in re.finditer(r'(!?)(\w+)\.contains\(' + LIT + r'\)', s):
            found_any = True
            negated = (m.group(1) == '!') ^ neg_all
            check(m.group(3), vars_.get(m.group(2)), not negated, s)
        # Regex("…").findAll(x).count() == 0  /  Regex("…").containsMatchIn(x)
        for m in re.finditer(r'Regex\(' + LIT + r'\)\.(findAll|containsMatchIn)\((\w+)\)(?:\.count\(\)\s*==\s*0)?', s):
            found_any = True
            path = vars_.get(m.group(3)); body = read(path) if path else None
            if body is None: continue
            try: rx = re.compile(unescape(m.group(1)).replace('\\\\', '\\'))
            except re.error: continue
            checked += 1
            hit = rx.search(body) is not None
            expect_hit = (m.group(2) == 'containsMatchIn') ^ neg_all
            if hit != expect_hit:
                bad += 1; print('PIN REGEX: %s -> %s : %s (hit=%s)' % (os.path.basename(t), path, m.group(1)[:80], hit))
        if not found_any and strict:
            unparsed += 1; print('UNPARSED (check by hand): %s : %s' % (os.path.basename(t), s[:140]))

if unresolved:
    print('check_test_pins: %d پین بدون فایل مرجع (بررسی دستی لازم)' % unresolved)
print('check_test_pins: %d missing/unexpected (%d pins checked%s%s)' % (bad, checked, (', %d unparsed' % unparsed) if strict else '', (', %d unresolved' % unresolved) if unresolved else ''))
sys.exit(1 if bad else 0)
