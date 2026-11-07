#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V350 — نگهبان کامل‌بودن `when` روی خانوادهٔ sealed متن غنی.

چرا: در Kotlin 2.0 اگر یک شاخه به `RichSegment` اضافه شود، هر `when` که روی
آن نوشته شده باید شاخهٔ تازه را هم داشته باشد، وگرنه **کامپایل** می‌شکند.
V350 شاخهٔ `Code` را افزود؛ این نگهبان تضمین می‌کند هیچ `when` جا نمانده باشد.

چه می‌سنجد: هر بلوک `when` که بدنه‌اش `is RichSegment.` دارد، باید یا شاخهٔ
همهٔ گونه‌ها را داشته باشد یا `else ->`.

اجرا:  python3 scripts/check_sealed_when.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / 'app/src'

FAMILY = 'RichSegment'
# گونه‌ها از خود منبع خوانده می‌شوند تا با رشد خانواده، نگهبان هم رشد کند.
MODEL = SRC / 'main/java/ir/exam/app/core/text/RichText.kt'


def variants() -> list:
    text = MODEL.read_text(encoding='utf-8')
    start = text.index('sealed interface ' + FAMILY)
    end = text.index('\n}', start)
    body = text[start:end]
    return re.findall(r'data class ([A-Za-z0-9_]+)\(', body)


def when_blocks(src: str):
    """بلوک‌های when با آکولادشماري درست (substringBefore جواب نمی‌دهد)."""
    for m in re.finditer(r'\bwhen\s*\(', src):
        i = src.find('{', m.end())
        if i < 0:
            continue
        depth = 0
        j = i
        while j < len(src):
            if src[j] == '{':
                depth += 1
            elif src[j] == '}':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        yield src[i:j + 1]


def main() -> int:
    want = variants()
    if not want:
        print('check_sealed_when: گونه‌ای پیدا نشد — نگهبان بی‌اثر است')
        return 1
    problems = []
    blocks = 0
    for path in sorted(SRC.rglob('*.kt')):
        src = path.read_text(encoding='utf-8')
        if 'is ' + FAMILY + '.' not in src:
            continue
        for body in when_blocks(src):
            if 'is ' + FAMILY + '.' not in body:
                continue
            blocks += 1
            if re.search(r'\belse\s*->', body):
                continue
            missing = [v for v in want if ('is %s.%s' % (FAMILY, v)) not in body]
            if missing:
                problems.append('%s: when شاخه‌های %s را ندارد'
                                % (path.relative_to(ROOT), '، '.join(missing)))
    if problems:
        print('check_sealed_when: %d مورد' % len(problems))
        for p in problems:
            print('  - ' + p)
        return 1
    print('check_sealed_when: سالم — %d بلوک when روی %s (%d گونه)'
          % (blocks, FAMILY, len(want)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
