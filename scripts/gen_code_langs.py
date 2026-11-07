#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V349 — تولید جدول زبان‌های کد برای اپ از همان منبع سایت.

چرا: جدول ۳۲ زبان یک‌بار در site/src/code.js نوشته شده است. اگر در کاتلین
دوباره دستی نوشته شود، دیر یا زود از سایت جدا می‌افتد. این اسکریپت با node
جدول را از خود فایل سایت می‌خواند و CodeLanguages.kt را می‌سازد.

اجرا:  python3 scripts/gen_code_langs.py
نیاز به node دارد (فقط روی ماشین توسعه؛ CI این را اجرا نمی‌کند).
"""
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'app/src/main/java/ir/exam/app/ui/code/CodeLanguages.kt'

DUMP = r"""
global.window = { ExamSite: { el: function () { return {style: {setProperty: function () {}}, addEventListener: function () {}, appendChild: function () {}}; },
  esc: function (s) { return String(s); }, toast: function () {} } };
global.document = { addEventListener: function () {}, createElement: function () { return {style: {}, setAttribute: function () {}}; },
  body: { appendChild: function () {} } };
global.navigator = {};
require(process.argv[2]);
var C = global.window.SiteCode;
var out = C.LANGS.map(function (L) {
  return {id: L.id, name: L.name, short: L.short, color: L.color, ext: L.ext, indent: L.indent,
          line: L.line, block: L.block, kw: L.kw, sample: L.sample,
          symbols: C.symbolsFor(L.id), snippets: C.snippetsFor(L.id).map(function (s) { return [s.label, s.code]; })};
});
process.stdout.write(JSON.stringify(out));
"""


def kstr(s: str) -> str:
    """رشتهٔ کاتلین: $ باید گریز بخورد وگرنه الگوی رشته تلقی می‌شود."""
    return ('"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$')
            .replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t') + '"')


def klist(items) -> str:
    return 'listOf(' + ', '.join(kstr(x) for x in items) + ')'


def main() -> int:
    js = ROOT / 'site/src/code.js'
    tmp = ROOT / 'scripts/_dump_langs.js'
    tmp.write_text(DUMP, encoding='utf-8')
    try:
        raw = subprocess.check_output(['node', str(tmp), str(js)], text=True)
    finally:
        tmp.unlink(missing_ok=True)
    langs = json.loads(raw)
    assert len(langs) == 32, len(langs)

    lines = [
        'package ir.exam.app.ui.code',
        '',
        '/**',
        ' * V349 — جدول زبان‌های محیط کدنویسی (۳۲ زبان).',
        ' * این فایل با scripts/gen_code_langs.py از site/src/code.js ساخته می‌شود؛',
        ' * دستی ویرایش نکنید تا اپ و سایت یکسان بمانند.',
        ' * کد اجرا نمی‌شود: این جدول فقط برای نوشتن، رنگی‌کردن و درج علائم است.',
        ' */',
        'data class CodeLanguage(',
        '    val id: String,',
        '    val name: String,',
        '    val short: String,',
        '    val colorArgb: Long,',
        '    val ext: String,',
        '    val indent: Int,',
        '    val lineComment: String,',
        '    val blockOpen: String,',
        '    val blockClose: String,',
        '    val keywords: List<String>,',
        '    val sample: String,',
        '    val symbols: List<String>,',
        '    val snippets: List<CodeSnippet>',
        ')',
        '',
        '/** قطعه‌کد آماده؛ نشان $0 جای نشانگر پس از درج است. */',
        'data class CodeSnippet(val label: String, val code: String)',
        '',
        'object CodeLanguages {',
        '    val all: List<CodeLanguage> = listOf(',
    ]
    for i, L in enumerate(langs):
        color = '0xFF' + L['color'].lstrip('#').upper()
        block = L['block'] or ['', '']
        snips = ', '.join('CodeSnippet(%s, %s)' % (kstr(a), kstr(b)) for a, b in L['snippets'])
        lines += [
            '        CodeLanguage(',
            '            id = %s, name = %s, short = %s,' % (kstr(L['id']), kstr(L['name']), kstr(L['short'])),
            '            colorArgb = %s, ext = %s, indent = %d,' % (color, kstr(L['ext']), L['indent']),
            '            lineComment = %s, blockOpen = %s, blockClose = %s,' % (
                kstr(L['line'] or ''), kstr(block[0]), kstr(block[1])),
            '            keywords = %s,' % klist([w for w in L['kw'].split() if w]),
            '            sample = %s,' % kstr(L['sample']),
            '            symbols = %s,' % klist(L['symbols']),
            '            snippets = listOf(%s)' % snips,
            '        )' + (',' if i < len(langs) - 1 else ''),
        ]
    lines += [
        '    )',
        '',
        '    private val byId: Map<String, CodeLanguage> = all.associateBy { it.id }',
        '',
        '    /** نام‌های جایگزینی که ممکن است در بلوک کد نوشته شده باشد. */',
        '    private val alias: Map<String, String> = mapOf(',
        '        "py" to "python", "python3" to "python", "js" to "javascript", "node" to "javascript",',
        '        "ts" to "typescript", "c++" to "cpp", "cxx" to "cpp", "c#" to "csharp", "cs" to "csharp",',
        '        "kt" to "kotlin", "kts" to "kotlin", "golang" to "go", "postgres" to "sql",',
        '        "postgresql" to "sql", "mysql" to "sql", "sqlite" to "sql", "rlang" to "r",',
        '        "htm" to "html", "html5" to "html", "scss" to "css", "sass" to "css", "rs" to "rust",',
        '        "rb" to "ruby", "shell" to "bash", "sh" to "bash", "zsh" to "bash", "octave" to "matlab",',
        '        "assembly" to "asm", "nasm" to "asm", "pas" to "pascal", "delphi" to "pascal",',
        '        "vb.net" to "vb", "vbnet" to "vb", "basic" to "vb", "sc" to "scala", "pl" to "perl",',
        '        "jl" to "julia", "f90" to "fortran", "f95" to "fortran", "swipl" to "prolog",',
        '        "scheme" to "lisp", "clisp" to "lisp", "elisp" to "lisp", "gvy" to "groovy",',
        '        "objective-c" to "objc", "objectivec" to "objc", "obj-c" to "objc"',
        '    )',
        '',
        '    val default: CodeLanguage get() = byId.getValue("python")',
        '',
        '    fun byIdOr(id: String?): CodeLanguage {',
        '        val key = id?.trim()?.lowercase().orEmpty()',
        '        return byId[key] ?: byId[alias[key]] ?: default',
        '    }',
        '}',
        '',
    ]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text('\n'.join(lines), encoding='utf-8')
    print('CodeLanguages.kt ساخته شد: %d زبان، %.1f کیلوبایت' % (len(langs), OUT.stat().st_size / 1024))
    return 0


if __name__ == '__main__':
    sys.exit(main())
