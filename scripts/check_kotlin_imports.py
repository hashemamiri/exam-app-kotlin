#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""نگهبان importهای کاتلین — خطای «Unresolved reference» را پیش از CI می‌گیرد.

چرا لازم شد: در V349 یک import اشتباه (QuestionType از بستهٔ نادرست) نوشتیم.
نگهبان‌های پرانتز/گیومه/گریز آن را نمی‌دیدند و CI با شکست کامپایل برگشت.
این اسکریپت نام‌های اعلام‌شدهٔ هر بستهٔ مخزن را فهرست می‌کند و هر import را
با آن می‌سنجد: اگر بسته در مخزن هست ولی نام در آن اعلام نشده، خطا است.

importهای بیرونی (androidx، kotlin، java، …) چون SDK اینجا نیست سنجیده
نمی‌شوند؛ ولی اگر جایی در مخزن سابقه نداشته باشند هشدار بازبینی می‌دهد.

اجرا:  python3 scripts/check_kotlin_imports.py
"""
import pathlib
import re
import sys
from collections import defaultdict

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC_ROOTS = [ROOT / 'app/src/main/java', ROOT / 'app/src/test/java']

DECL = re.compile(
    r'^\s*(?:@\w+\s+)*(?:public\s+|internal\s+|private\s+|abstract\s+|open\s+|sealed\s+|data\s+|value\s+|enum\s+|annotation\s+|inline\s+|expect\s+|actual\s+)*'
    r'(?:class|interface|object|typealias)\s+([A-Za-z_][A-Za-z0-9_]*)', re.M)
# توابع/ویژگی‌های افزوده (receiver) هم باید شمرده شوند: fun Context.findActivityContext()
MODS = r'(?:@\w+(?:\([^)]*\))?\s+)*(?:public\s+|internal\s+|private\s+|protected\s+)?(?:inline\s+|suspend\s+|operator\s+|infix\s+|tailrec\s+|external\s+|const\s+|lateinit\s+|override\s+)*'
RECV = r'(?:[\w.<>,?\s]+\.)?'
TOP_FN = re.compile(r'^\s*' + MODS + r'fun\s+(?:<[^>]+>\s*)?' + RECV + r'([A-Za-z_][A-Za-z0-9_]*)\s*\(', re.M)
TOP_PROP = re.compile(r'^\s*' + MODS + r'(?:val|var)\s+(?:<[^>]+>\s*)?' + RECV + r'([A-Za-z_][A-Za-z0-9_]*)', re.M)
# کلاس‌هایی که بیلد می‌سازد و در سورس نیستند
GENERATED = {'BuildConfig', 'R', 'Manifest'}
PKG = re.compile(r'^package\s+([A-Za-z0-9_.]+)', re.M)
IMPORT = re.compile(r'^import\s+([A-Za-z0-9_.]+)(?:\s+as\s+\w+)?\s*$', re.M)


def collect():
    declared = defaultdict(set)
    files = []
    for base in SRC_ROOTS:
        if not base.exists():
            continue
        for path in sorted(base.rglob('*.kt')):
            text = path.read_text(encoding='utf-8')
            m = PKG.search(text)
            if not m:
                continue
            pkg = m.group(1)
            names = set(DECL.findall(text)) | set(TOP_FN.findall(text)) | set(TOP_PROP.findall(text))
            declared[pkg] |= names
            files.append((path, pkg, text))
    return declared, files


def main() -> int:
    declared, files = collect()
    errors = []
    seen_imports = set()
    for _, _, text in files:
        seen_imports |= set(IMPORT.findall(text))

    warnings = []
    for path, pkg, text in files:
        for imp in IMPORT.findall(text):
            if imp.endswith('.*'):
                base = imp[:-2]
                if base in declared or base.rsplit('.', 1)[0] in declared:
                    continue
                continue
            if '.' not in imp:
                continue
            base, name = imp.rsplit('.', 1)
            if name in GENERATED:
                continue
            if base in declared:
                # نام باید در همان بسته اعلام شده باشد
                if name not in declared[base]:
                    # عضو یک object هم مجاز است: Base.Name که Base در بسته باشد
                    owner = base.rsplit('.', 1)[-1]
                    if owner in declared.get(base.rsplit('.', 1)[0], set()):
                        continue
                    errors.append('%s: import %s — نام «%s» در بستهٔ %s نیست' % (
                        path.relative_to(ROOT), imp, name, base))
                continue
            if base.startswith('ir.exam.app') or base.startswith('ir.onlineexam'):
                # عضوِ تودرتو (مثل FigureKind.GEOMETRY یا ShapeRecognizer.Pt):
                # اگر والد بسته باشد و نامِ کلاس در آن اعلام شده باشد، مجاز است.
                parent, _, owner = base.rpartition('.')
                if parent in declared and owner in declared[parent]:
                    continue
                if base not in declared:
                    errors.append('%s: import %s — بستهٔ %s در مخزن نیست' % (
                        path.relative_to(ROOT), imp, base))
                continue
            if imp not in seen_imports:
                warnings.append('%s: import بیرونی بی‌سابقه %s' % (path.relative_to(ROOT), imp))

    for e in errors:
        print('❌ ' + e)
    for w in warnings[:10]:
        print('⚠ ' + w)
    total = sum(len(IMPORT.findall(t)) for _, _, t in files)
    if errors:
        print('check_kotlin_imports: %d خطا از %d import در %d فایل' % (len(errors), total, len(files)))
        return 1
    print('check_kotlin_imports: سالم — %d import در %d فایل' % (total, len(files)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
