#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V350 — نگهبان «ایمپورت جامانده» در فایل‌های Compose.

چرا: check_kotlin_import.py فقط ایمپورت‌های *نوشته‌شده* را می‌سنجد. اگر نمادی
مثل Text استفاده شود ولی ایمپورتش نوشته نشده باشد، آن نگهبان سبز می‌ماند و
کامپایل CI می‌شکند. این نگهبان دقیقاً همان شکاف را می‌بندد.

چه می‌سنجد: نمادهای پرکاربرد Compose/Material3 که در فایل «فراخوانی» شده‌اند
(به‌شکل `Name(`) ولی نه ایمپورت شده‌اند، نه در همان فایل تعریف شده‌اند، و نه
از بستهٔ خود فایل هستند.

چه نمی‌سنجد: نمادهایی که با نام کامل صدا زده می‌شوند، یا سازندهٔ کلاس هم‌نام.
برای پرهیز از مثبت کاذب، فقط فهرست زیر بررسی می‌شود و هر مورد باید در همان
فایل تعریف نشده باشد.

اجرا:  python3 scripts/check_missing_imports.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / 'app/src/main/java/ir/exam/app'

# نماد → بستهٔ درست. فقط نمادهایی که واقعاً در این پروژه به کار می‌روند.
SYMBOLS = {
    'Text': 'androidx.compose.material3.Text',
    'Icon': 'androidx.compose.material3.Icon',
    'IconButton': 'androidx.compose.material3.IconButton',
    'Surface': 'androidx.compose.material3.Surface',
    'MaterialTheme': 'androidx.compose.material3.MaterialTheme',
    'Button': 'androidx.compose.material3.Button',
    'OutlinedButton': 'androidx.compose.material3.OutlinedButton',
    'TextButton': 'androidx.compose.material3.TextButton',
    'Card': 'androidx.compose.material3.Card',
    'Divider': 'androidx.compose.material3.Divider',
    'HorizontalDivider': 'androidx.compose.material3.HorizontalDivider',
    'CircularProgressIndicator': 'androidx.compose.material3.CircularProgressIndicator',
    'LinearProgressIndicator': 'androidx.compose.material3.LinearProgressIndicator',
    'Checkbox': 'androidx.compose.material3.Checkbox',
    'RadioButton': 'androidx.compose.material3.RadioButton',
    'Switch': 'androidx.compose.material3.Switch',
    'Slider': 'androidx.compose.material3.Slider',
    'AlertDialog': 'androidx.compose.material3.AlertDialog',
    'Box': 'androidx.compose.foundation.layout.Box',
    'Column': 'androidx.compose.foundation.layout.Column',
    'Row': 'androidx.compose.foundation.layout.Row',
    'Spacer': 'androidx.compose.foundation.layout.Spacer',
    'FlowRow': 'androidx.compose.foundation.layout.FlowRow',
    'BasicTextField': 'androidx.compose.foundation.text.BasicTextField',
    'Image': 'androidx.compose.foundation.Image',
    'Canvas': 'androidx.compose.foundation.Canvas',
    'BorderStroke': 'androidx.compose.foundation.BorderStroke',
    'RoundedCornerShape': 'androidx.compose.foundation.shape.RoundedCornerShape',
    'CircleShape': 'androidx.compose.foundation.shape.CircleShape',
    'SolidColor': 'androidx.compose.ui.graphics.SolidColor',
    'Color': 'androidx.compose.ui.graphics.Color',
    'Alignment': 'androidx.compose.ui.Alignment',
    'Modifier': 'androidx.compose.ui.Modifier',
    'FontWeight': 'androidx.compose.ui.text.font.FontWeight',
    'FontFamily': 'androidx.compose.ui.text.font.FontFamily',
    'TextStyle': 'androidx.compose.ui.text.TextStyle',
    'TextAlign': 'androidx.compose.ui.text.style.TextAlign',
    'AnnotatedString': 'androidx.compose.ui.text.AnnotatedString',
    'SpanStyle': 'androidx.compose.ui.text.SpanStyle',
    'buildAnnotatedString': 'androidx.compose.ui.text.buildAnnotatedString',
    'Composable': 'androidx.compose.runtime.Composable',
    'LaunchedEffect': 'androidx.compose.runtime.LaunchedEffect',
    'DisposableEffect': 'androidx.compose.runtime.DisposableEffect',
    'remember': 'androidx.compose.runtime.remember',
    'mutableStateOf': 'androidx.compose.runtime.mutableStateOf',
    'rememberScrollState': 'androidx.compose.foundation.rememberScrollState',
    'verticalScroll': 'androidx.compose.foundation.verticalScroll',
    'horizontalScroll': 'androidx.compose.foundation.horizontalScroll',
    'clickable': 'androidx.compose.foundation.clickable',
    'background': 'androidx.compose.foundation.background',
    'border': 'androidx.compose.foundation.border',
    'padding': 'androidx.compose.foundation.layout.padding',
    'size': 'androidx.compose.foundation.layout.size',
    'height': 'androidx.compose.foundation.layout.height',
    'width': 'androidx.compose.foundation.layout.width',
    'heightIn': 'androidx.compose.foundation.layout.heightIn',
    'widthIn': 'androidx.compose.foundation.layout.widthIn',
    'fillMaxWidth': 'androidx.compose.foundation.layout.fillMaxWidth',
    'fillMaxSize': 'androidx.compose.foundation.layout.fillMaxSize',
    'fillMaxHeight': 'androidx.compose.foundation.layout.fillMaxHeight',
    'Arrangement': 'androidx.compose.foundation.layout.Arrangement',
}

# نکته: `(?<![A-Za-z0-9_.])` باعث می‌شود فراخوانی عضوی مثل `Modifier.padding(`
# نماد مستقل شمرده نشود؛ فقط نامِ تنها در ابتدای عبارت.
CALL = re.compile(r'(?<![A-Za-z0-9_.])([A-Z][A-Za-z0-9_]*)\s*\(')
# V352 — تابع‌های افزایشی که با «نقطه» صدا زده می‌شوند و ایمپورت جدا می‌خواهند.
# این دسته از قاعدهٔ بالا جستار نمی‌شد (چون پس از نقطه است) و یک‌بار در
# CodeEditorDialog باعث خطای کامپایل شد: headerTile.luminance() بی‌ایمپورت.
EXT_CALL = re.compile(r'\.([a-z][A-Za-z0-9_]*)\s*\(')
EXTENSIONS = {
    'luminance': 'androidx.compose.ui.graphics.luminance',
    'copy': None,  # متد عضو است، نه افزونه
}
EXTENSIONS = {k: v for k, v in EXTENSIONS.items() if v}
IMPORT = re.compile(r'^import\s+([A-Za-z0-9_.]+)(?:\s+as\s+([A-Za-z0-9_]+))?\s*$', re.M)
DECL = re.compile(r'\b(?:class|object|interface|enum class|data class|fun)\s+([A-Za-z0-9_]+)')


def strip_noise(src: str) -> str:
    """رشته‌ها و توضیحات حذف می‌شوند تا نام داخل متن، فراخوانی شمرده نشود."""
    out = []
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c == '"' and src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            out.append(' ')
            continue
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == '\\':
                    i += 1
                i += 1
            i += 1
            out.append(' ')
            continue
        if src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def main() -> int:
    problems = []
    scanned = 0
    for path in sorted(SRC.rglob('*.kt')):
        raw = path.read_text(encoding='utf-8')
        if '@Composable' not in raw:
            continue
        scanned += 1
        body = strip_noise(raw)
        imports = {}
        for m in IMPORT.finditer(raw):
            full, alias = m.group(1), m.group(2)
            imports[alias or full.rsplit('.', 1)[-1]] = full
        declared = set(DECL.findall(body))
        for m in EXT_CALL.finditer(body):
            name = m.group(1)
            if name not in EXTENSIONS:
                continue
            if name in imports or name in declared:
                continue
            problems.append('%s: %s() استفاده شده ولی ایمپورت نشده (باید %s باشد)'
                            % (path.relative_to(ROOT), name, EXTENSIONS[name]))
        for m in CALL.finditer(body):
            name = m.group(1)
            if name not in SYMBOLS:
                continue
            if name in imports or name in declared:
                continue
            problems.append('%s: %s استفاده شده ولی ایمپورت نشده (باید %s باشد)'
                            % (path.relative_to(ROOT), name, SYMBOLS[name]))
    if problems:
        print('check_missing_imports: %d مورد' % len(problems))
        for p in problems[:40]:
            print('  - ' + p)
        return 1
    print('check_missing_imports: سالم — %d فایل Compose بررسی شد' % scanned)
    return 0


if __name__ == '__main__':
    sys.exit(main())
