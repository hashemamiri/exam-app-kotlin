#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V350.1 — نگهبان «ریشهٔ مخزن» در تست‌های واحد.

چرا: دایرکتوری کاریِ `testDebugUnitTest` ریشهٔ مخزن نیست (پوشهٔ app است).
تستی که فایل مخزن را با مسیر نسبیِ تنها باز می‌کند، در CI پیدا نمی‌کند و
می‌شکند. V350 دقیقاً به همین دلیل ۱۶ آزمون را شکست، در حالی که محلی درست
به نظر می‌رسید.

الگوی درست پروژه (V348/V160): بالا رفتن تا settings.gradle.kts، سپس
File(root, path).

قاعده: هر فایل تستی که فایل مخزن را «می‌خواند» یا «وجودش را می‌سنجد»، باید
ریشه را پیدا کند — یعنی نام settings.gradle.kts در خودش باشد.

مستثنا (مثبت کاذب نشود):
  • فهرست مسیرهای جایگزین: listOf(File("src/…"), File("app/src/…")) که با
    isDirectory انتخاب می‌شود (الگوی V335_2).
  • سنجش پوچ: File("…").let { true } که هرگز شکست نمی‌خورد (V160_2).

اجرا:  python3 scripts/check_test_paths.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TESTS = ROOT / 'app/src/test'

TOPS = ('site/', 'app/', 'scripts/', 'docs/', 'text/', 'sql/', 'relay/',
        'supabase/', '.github/', 'gradle/')

ROOT_MARK = 'settings.gradle.kts'

# File("repo/…") یا File(identifier) — تک‌آرگومانی
BARE = re.compile(r'\bFile\(\s*("(?:[^"\\]|\\.)*"|[A-Za-z_][A-Za-z0-9_]*)\s*\)')
# خواندن/سنجش وجود، روی همان File یا روی متغیرش
USES = ('readText', 'exists()', 'isFile', 'isDirectory', 'walkTopDown',
        'listFiles', 'inputStream', 'readLines', 'bufferedReader')


def strip_comments(src: str) -> str:
    """توضیحات حذف می‌شوند؛ وگرنه نام settings.gradle.kts در یک کامنت،
    نگهبان را راضی می‌کرد در حالی که کد اصلاً ریشه را پیدا نمی‌کند."""
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            out.append(' ')
            continue
        if src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if src[i] == '"':
            if src.startswith('"""', i):
                j = src.find('"""', i + 3)
                out.append(src[i:(n if j < 0 else j + 3)])
                i = n if j < 0 else j + 3
                continue
            j = i + 1
            while j < n and src[j] != '"':
                if src[j] == '\\':
                    j += 1
                j += 1
            out.append(src[i:j + 1])
            i = j + 1
            continue
        out.append(src[i])
        i += 1
    return ''.join(out)


def needs_root(text: str) -> bool:
    """آیا این فایل تست، فایل مخزن را از مسیر جاری باز می‌کند؟"""
    lines = text.splitlines()
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
            continue
        if 'listOf(' in line:
            continue  # فهرست مسیرهای جایگزین
        for m in BARE.finditer(line):
            arg = m.group(1)
            if arg.startswith('"'):
                lit = arg[1:-1]
                if not lit.startswith(TOPS):
                    continue
            tail = line[m.end():]
            if tail.lstrip().startswith('.let { true }'):
                continue  # سنجش پوچ
            # مصرف مستقیم روی همان عبارت
            if any(u in tail for u in USES):
                return True
            # یا انتساب به متغیر و مصرف در چند خط بعد
            vm = re.search(r'val\s+(\w+)\s*=\s*' + re.escape(m.group(0)), line)
            if vm:
                window = '\n'.join(lines[i:i + 8])
                if re.search(r'\b' + re.escape(vm.group(1)) + r'\.(?:' +
                             '|'.join(re.escape(u.rstrip('()')) for u in USES) + r')', window):
                    return True
    return False


def main() -> int:
    problems = []
    scanned = 0
    for path in sorted(TESTS.rglob('*.kt')):
        text = strip_comments(path.read_text(encoding='utf-8'))
        scanned += 1
        if ROOT_MARK in text:
            continue  # ریشه را واقعاً در کد پیدا می‌کند
        if needs_root(text):
            problems.append(str(path.relative_to(ROOT)))
    if problems:
        print('check_test_paths: %d فایل ریشهٔ مخزن را پیدا نمی‌کند' % len(problems))
        for p in problems:
            print('  - ' + p)
        print('  راه درست: تا settings.gradle.kts بالا بروید و File(root, path) بسازید.')
        return 1
    print('check_test_paths: سالم — %d فایل تست بررسی شد' % scanned)
    return 0


if __name__ == '__main__':
    sys.exit(main())
