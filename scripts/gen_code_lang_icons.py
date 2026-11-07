#!/usr/bin/env python3
"""V351.2 — ساخت CodeLanguageIcons.kt از مسیرهای SVG.

۲۷ نشان از simple-icons (CC0) گرفته شده و در همان فایل نشان‌های رسمی
نگه داشته می‌شود. پنج زبان نشان رسمی ندارند (C#، Objective-C، Assembly،
Visual Basic، Prolog)؛ برایشان نشان سادهٔ خطی طراحی شده است.
مسیرها در خود فایل کاتلین جاسازی می‌شوند؛ در زمان ساخت هیچ شبکه‌ای لازم نیست.

اجرا: python3 scripts/gen_code_lang_icons.py
"""
import json, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parent.parent
# منبعِ نشان‌ها داخل مخزن است تا ساخت به شبکه یا /tmp نیاز نداشته باشد.
SRC = ROOT / 'docs/tools/code_lang_icons.json'

# نشان‌های جایگزین که نشان خودِ زبان نیستند و باید طراحی شوند
CUSTOM = {
    'csharp': 'M13 8 A5 5 0 1 0 13 16 M17.5 7 L16.3 17 M21 7 L19.8 17 M15.4 10.2 H22 M15 13.8 H21.6',
    'objc': 'M9.5 5 H5.5 V19 H9.5 M14.5 5 H18.5 V19 H14.5 M12 10 A2 2 0 1 0 12 14 A2 2 0 1 0 12 10',
    'asm': 'M7 7 H17 V17 H7 Z M10 4 V7 M14 4 V7 M10 17 V20 M14 17 V20 M4 10 H7 M4 14 H7 M17 10 H20 M17 14 H20',
    'vb': 'M4 6 H20 V18 H4 Z M4 9.5 H20 M7 13 H12 M7 15.5 H15',
    'prolog': ('M12 5 A1.6 1.6 0 1 0 12 8.2 A1.6 1.6 0 1 0 12 5 M12 8.2 L6.5 14 '
               'M12 8.2 L17.5 14 M6.5 14 A1.6 1.6 0 1 0 6.5 17.2 A1.6 1.6 0 1 0 6.5 14 '
               'M17.5 14 A1.6 1.6 0 1 0 17.5 17.2 A1.6 1.6 0 1 0 17.5 14'),
}

def main():
    data = json.loads(SRC.read_text(encoding='utf-8'))['languages']
    ids = re.findall(r'id = "([a-z]+)"', (ROOT/'app/src/main/java/ir/exam/app/ui/code/CodeLanguages.kt').read_text(encoding='utf-8'))
    assert len(ids) == 32, len(ids)
    out = {}
    for lid in ids:
        assert lid in data, lid
        rec = data[lid]
        if rec['stroked']:
            # نشان طراحی‌شده باید با ثابت بالای همین فایل یکی باشد
            assert CUSTOM[lid] == rec['d'], lid
        out[lid] = (rec['d'], bool(rec['stroked']))
    for lid, (d, _) in out.items():
        assert '"' not in d and '\\' not in d and '$' not in d, lid
    L = [
        'package ir.exam.app.ui.code',
        '',
        'import androidx.compose.ui.graphics.Color',
        'import androidx.compose.ui.graphics.SolidColor',
        'import androidx.compose.ui.graphics.StrokeCap',
        'import androidx.compose.ui.graphics.StrokeJoin',
        'import androidx.compose.ui.graphics.vector.ImageVector',
        'import androidx.compose.ui.graphics.vector.addPathNodes',
        'import androidx.compose.ui.unit.dp',
        '',
        '/**',
        ' * V351.2 — آیکن SVG هر زبان محیط کدنویسی.',
        ' * این فایل با scripts/gen_code_lang_icons.py ساخته می‌شود؛ دستی ویرایش نکنید.',
        ' * ۲۷ نشان رسمی از simple-icons (پروانهٔ CC0) و ۵ نشان خطیِ طراحی‌شده',
        ' * برای زبان‌هایی که نشان رسمی ندارند: C#، Objective-C، Assembly،',
        ' * Visual Basic و Prolog. همه در دستگاه مختصات ۲۴×۲۴ هستند.',
        ' * رنگ در محل استفاده گذاشته می‌شود، پس هر نشان یک‌بار ساخته و نگه داشته می‌شود.',
        ' */',
        'internal object CodeLanguageIcons {',
        '',
        '    /** مسیر SVG و اینکه خطی است (نه پُر). */',
        '    private data class Mark(val d: String, val stroked: Boolean)',
        '',
        '    private val marks: Map<String, Mark> = mapOf(',
    ]
    for lid in ids:
        d, st = out[lid]
        L.append('        "%s" to Mark("%s", %s),' % (lid, d, 'true' if st else 'false'))
    L += [
        '    )',
        '',
        '    private val cache = mutableMapOf<String, ImageVector>()',
        '',
        '    /** نشان زبان؛ اگر زبانی نشان نداشت، نشان عمومی کد برمی‌گردد. */',
        '    fun of(langId: String): ImageVector = cache.getOrPut(langId) {',
        '        val mark = marks[langId] ?: marks.getValue("asm")',
        '        val nodes = addPathNodes(mark.d)',
        '        ImageVector.Builder(',
        '            defaultWidth = 24.dp,',
        '            defaultHeight = 24.dp,',
        '            viewportWidth = 24f,',
        '            viewportHeight = 24f',
        '        ).apply {',
        '            if (mark.stroked) {',
        '                addPath(',
        '                    pathData = nodes,',
        '                    stroke = SolidColor(Color.White),',
        '                    strokeLineWidth = 1.8f,',
        '                    strokeLineCap = StrokeCap.Round,',
        '                    strokeLineJoin = StrokeJoin.Round',
        '                )',
        '            } else {',
        '                addPath(pathData = nodes, fill = SolidColor(Color.White))',
        '            }',
        '        }.build()',
        '    }',
        '',
        '    /** مسیر SVG خام زبان؛ برای آزمونِ دادهٔ نشان (بدون API کامپوز). */',
        '    fun pathOf(langId: String): String = (marks[langId] ?: marks.getValue("asm")).d',
        '',
        '    /** آیا این زبان نشان شناخته‌شده دارد؟ */',
        '    fun has(langId: String): Boolean = marks.containsKey(langId)',
        '',
        '    /** چند زبان نشان خطی دارند (برای آزمون). */',
        '    fun isStroked(langId: String): Boolean = marks[langId]?.stroked == true',
        '',
        '    /** شمار نشان‌ها؛ باید با شمار زبان‌ها یکی باشد. */',
        '    val count: Int get() = marks.size',
        '}',
        '',
    ]
    dst = ROOT/'app/src/main/java/ir/exam/app/ui/code/CodeLanguageIcons.kt'
    dst.write_text('\n'.join(L), encoding='utf-8')
    print('CodeLanguageIcons.kt ساخته شد: %d نشان (%.1f کیلوبایت)، %d خطی' % (
        len(out), dst.stat().st_size/1024, sum(1 for _, s in out.values() if s)))
    write_js(data, ids)

BEGIN = '  /* BEGIN GENERATED LANG ICONS'
END = '  /* END GENERATED LANG ICONS */'
TPL = ROOT / 'docs/tools/code_icons_js_template.txt'


def write_js(data, ids):
    """همان منبع، این‌بار برای سایت؛ تا اپ و سایت نشان یکسان داشته باشند.
    قالب در پروندهٔ جداست تا درگیر فرارِ تودرتوی نقل‌قول نشویم."""
    rows = ',\n'.join(
        "    %s: {d: '%s', s: %d}" % (lid, data[lid]['d'], 1 if data[lid]['stroked'] else 0)
        for lid in ids
    )
    block = TPL.read_text(encoding='utf-8').replace('@@ROWS@@', rows).rstrip('\n')
    p = ROOT / 'site/src/code.js'
    t = p.read_text(encoding='utf-8')
    if BEGIN in t and END in t:
        i, j = t.index(BEGIN), t.index(END) + len(END)
        t = t[:i] + block + t[j:]
    else:
        # نخستین IIFE فایل (بقیهٔ موردها تودرتو هستند)
        lines = t.split('\n')
        at = next(i for i, l in enumerate(lines) if l.strip() == '(function () {')
        lines.insert(at + 1, block)
        t = '\n'.join(lines)
    a2 = '    completions: completions, MAX: MAX'
    assert t.count(a2) == 1
    if 'icon: iconSvg' not in t:
        t = t.replace(a2, '    completions: completions, MAX: MAX, icon: iconSvg, inkOn: inkOn')
    p.write_text(t, encoding='utf-8')
    print('نشان‌های سایت هم ساخته شد: %d نشان در site/src/code.js' % len(ids))


if __name__ == '__main__':
    main()
