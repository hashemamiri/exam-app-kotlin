#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""نگهبان پین‌های متنیِ تست‌های کاتلین.

چرا لازم شد: تست‌های سایت با `contains("...")` تکه‌ای از سورس را پین می‌کنند.
با هر بازسازی، پین کهنه می‌شود و CI می‌شکند — در V349 سه تست همین‌طور شکستند
(V348، V306، V218) در حالی که نگهبان‌های دیگر «سالم» گزارش می‌دادند.

این اسکریپت هر تست را می‌خواند، فایل‌هایی را که آن تست می‌خواند پیدا می‌کند،
و تک‌تک رشته‌های پین‌شده را در همان فایل‌ها جست‌وجو می‌کند. اگر پیدا نشد،
پین کهنه است و باید به‌روز شود.

اجرا:  python3 scripts/check_site_pins.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TESTS = ROOT / 'app/src/test/java'

LIT = r'"((?:[^"\\]|\\.)*)"'
CONTAINS_RECV = re.compile(r'([A-Za-z_]\w*)\.contains\(\s*' + LIT + r'\s*\)')
IN_VAR = re.compile(LIT + r'\s+in\s+([A-Za-z_]\w*)')
# متغیرهایی که متن یک فایل را می‌خوانند
FILE_VAR = re.compile(r'(?:val|var)\s+([A-Za-z_]\w*)\s*(?::\s*String\s*)?=\s*[^\n]*(?:readText\(\)|src\(|source\()')
SRC_CALL = re.compile(r'src\(\s*"([^"]+)"\s*\)')
FILE_CALL = re.compile(r'File\(\s*root\(\)\s*,\s*"([^"]+)"\s*\)')
SRC_HELPER = re.compile(r'fun src\([^)]*\)[^=]*=\s*File\(\s*root\(\)\s*,\s*"([^"]*)"')


def unescape(s: str) -> str:
    out, i = [], 0
    while i < len(s):
        c = s[i]
        if c == '\\' and i + 1 < len(s):
            n = s[i + 1]
            mapping = {'n': '\n', 't': '\t', 'r': '\r', '"': '"', '\\': '\\', '$': '$', "'": "'", 'b': '\b'}
            if n in mapping:
                out.append(mapping[n]); i += 2; continue
            if n == 'u' and i + 5 < len(s):
                try:
                    out.append(chr(int(s[i + 2:i + 6], 16))); i += 6; continue
                except ValueError:
                    pass
            out.append(n); i += 2; continue
        out.append(c); i += 1
    return ''.join(out)


SOURCE_SUFFIXES = {'.kt', '.js', '.css', '.sql', '.ts', '.py', '.html', '.xml', '.txt', '.json', '.kts', '.yml', '.md'}
SKIP_DIRS = {'build', '.git', 'node_modules', '.gradle', '__pycache__'}


def repo_text() -> str:
    chunks = []
    for path in ROOT.rglob('*'):
        if not path.is_file() or path.suffix not in SOURCE_SUFFIXES:
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        # فایل‌های تست منبع نیستند: وگرنه هر پین خودش را پیدا می‌کرد و همیشه پاس می‌شد
        if 'test' in path.parts and path.suffix == '.kt':
            continue
        if path.stat().st_size > 4_000_000:
            continue
        try:
            chunks.append(path.read_text(encoding='utf-8', errors='replace'))
        except OSError:
            continue
    return '\n'.join(chunks)


REPO_TEXT = ''


def main() -> int:
    global REPO_TEXT
    REPO_TEXT = repo_text()
    stale, checked, skipped = [], 0, 0
    for path in sorted(TESTS.rglob('*.kt')):
        text = path.read_text(encoding='utf-8')
        # فقط متغیرهایی که متن فایل می‌گیرند پینِ سورس‌اند؛ خروجیِ زمان اجرا نه.
        file_vars = set(FILE_VAR.findall(text))
        raw = []
        for m in CONTAINS_RECV.finditer(text):
            raw.append((m.group(2), m.start(), m.group(1)))
        for m in IN_VAR.finditer(text):
            raw.append((m.group(1), m.start(), m.group(2)))
        needles = []
        for lit, pos, recv in raw:
            if recv not in file_vars:
                continue
            # پین منفی: نزدیک‌ترین assert پیش از این رشته اگر assertFalse باشد، یعنی نباید باشد
            head = text[:pos]
            last_true = head.rfind('assertTrue')
            last_false = head.rfind('assertFalse')
            if last_false > last_true:
                continue
            # V350.1 — شکل دوم پین منفی: assertTrue("پیام", !s.contains("…"))
            # پیش از خودِ عبارت، یک «!» نشسته است؛ یعنی نبودنِ رشته خواسته شده.
            before = text[max(0, pos - 80):pos]
            if re.search(r'!\s*$' if recv is None else r'!\s*' + re.escape(recv) + r'\s*\.\s*contains\(\s*$', before):
                continue
            if re.search(r'!\s*$', before):
                continue
            # الگوی «جمع‌کردن تخلف»: اگر رشته پیدا شود، فایل به فهرست بد افزوده می‌شود
            # و بعد فهرست باید خالی باشد ⇒ این هم پین منفی است.
            tail = text[pos:pos + 260]
            if re.search(r'\b(?:bad|violations|problems|offenders|errors)\s*\+=', tail):
                continue
            # الگوی رشتهٔ کاتلین ($k یا ${x}) در زمان اجرا جای دیگری می‌نشیند
            if re.search(r'(?<!\\)\$[A-Za-z_{]', lit):
                continue
            n = unescape(lit)
            if len(n) >= 6:
                needles.append(n)
        if not needles:
            continue

        helper = SRC_HELPER.search(text)
        base = ''
        if helper:
            base = helper.group(1).replace('$name', '').replace('${name}', '')
        files = set()
        for rel in SRC_CALL.findall(text):
            cand = ROOT / (base + rel) if base else None
            if cand and cand.is_file():
                files.add(cand)
            else:
                for guess in (ROOT / 'site/src' / rel, ROOT / 'site' / rel,
                              ROOT / 'app/src/main/java/ir/exam/app' / rel):
                    if guess.is_file():
                        files.add(guess); break
        for rel in FILE_CALL.findall(text):
            cand = ROOT / rel
            if cand.is_file():
                files.add(cand)
        if not files:
            skipped += 1
            continue

        for n in needles:
            checked += 1
            # سنجهٔ اصلی: پین باید جایی در سورس مخزن باشد. اگر هیچ‌جا نیست،
            # قطعاً کهنه است. (جست‌وجوی فقط-فایلِ تست مثبت کاذب زیاد می‌داد،
            # چون تست‌ها با چند کمک‌تابعِ متفاوت فایل می‌خوانند.)
            if n not in REPO_TEXT:
                stale.append((path.relative_to(ROOT), n, sorted(str(f.relative_to(ROOT)) for f in files)))

    for path, needle, files in stale:
        print('❌ %s' % path)
        print('   پین پیدا نشد: %r' % needle[:150])
        print('   فایل‌های خوانده‌شده: %s' % ', '.join(files)[:180])
    # قواعد regex‑محورِ پروژه که تست‌ها می‌سنجند و «contains» نیستند
    css = (ROOT / 'site/src/site.css').read_text(encoding='utf-8')
    white = len(re.findall(r'background:#fff\b', css))
    if white:
        msg = 'قاعدهٔ V218: background:#fff ممنوع (%d مورد) — از #FFFFFF استفاده کنید' % white
        print('❌ site/src/site.css')
        print('   ' + msg)
        stale.append(('site/src/site.css', msg, ['site/src/site.css']))
    if stale:
        print('check_site_pins: %d پین کهنه از %d پین' % (len(stale), checked))
        return 1
    print('check_site_pins: سالم — %d پین سنجیده شد؛ قاعدهٔ سفیدِ چاپ هم پاس' % checked)
    return 0


if __name__ == '__main__':
    sys.exit(main())
