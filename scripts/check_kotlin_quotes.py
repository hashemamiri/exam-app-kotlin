#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.50 — رشتهٔ کوتلینِ ناتمام و بک‌اسلشِ بیرونِ رشته (خطاهای فقط-CI).

درسِ V335.49: در یک تست، رشته‌ای با کوتیشنِ تودرتو نوشته شد
(`"cur.text.trimEnd() + "\\n" + clean"`) و کامپایلر فقط در CI شکست
(`Expecting ')'`). دو نشانهٔ قطعیِ این کلاس خطا بررسی می‌شود:
  ۱) رشتهٔ معمولی که تا انتهای خط بسته نشود (کوتلین رشتهٔ چندخطی ندارد)؛
  ۲) بک‌اسلش در «کد» (بیرون از رشته/کاراکتر/کامنت) — همان چیزی که رشتهٔ تودرتو
     تولید می‌کند.
رشته‌های سه‌کوتیشن (raw)، کاراکترها، کامنت‌ها و قالب‌های `${…}` (که خودشان
می‌توانند رشتهٔ تودرتو داشته باشند) درست رد می‌شوند.

کاربرد:  python3 scripts/check_kotlin_quotes.py [مسیر...]
"""
import pathlib
import sys


def scan(text, name):
    problems = []
    mode = "code"          # code | str | raw | char | lcomment | bcomment
    tdepth = []            # پشتهٔ عمق ${ ... } — بیرون‌آمدن یعنی بازگشت به رشته
    i, n, line = 0, len(text), 1

    def quote_run(start):
        k = 0
        while start + k < n and text[start + k] == '"':
            k += 1
        return k

    while i < n:
        c = text[i]
        if c == "\n":
            if mode == "str":
                problems.append((line, "رشتهٔ بسته‌نشده تا انتهای خط"))
                mode = "code"
                tdepth.clear()
            elif mode == "lcomment":
                mode = "code"
            line += 1
            i += 1
            continue

        if mode == "str":
            if c == "\\":
                i += 2
                continue
            if c == '"':
                mode = "code"
                i += 1
                continue
            if text.startswith("${", i):
                tdepth.append(1)
                mode = "code"
                i += 2
                continue
            i += 1
            continue

        if mode == "raw":
            if c == '"':
                k = quote_run(i)
                if k >= 3:
                    mode = "code"   # سه کوتیشن آخر می‌بندد؛ کوتیشن‌های اضافه متن‌اند
                i += k
                continue
            i += 1
            continue

        if mode == "char":
            if c == "\\":
                i += 2
                continue
            if c == "'":
                mode = "code"
            i += 1
            continue

        if mode == "lcomment":
            i += 1
            continue

        if mode == "bcomment":
            if text.startswith("*/", i):
                mode = "code"
                i += 2
                continue
            i += 1
            continue

        # ---- code ----
        if text.startswith("//", i):
            mode = "lcomment"
            i += 2
            continue
        if text.startswith("/*", i):
            mode = "bcomment"
            i += 2
            continue
        if text.startswith('"""', i):
            mode = "raw"
            i += 3
            continue
        if c == '"':
            mode = "str"
            i += 1
            continue
        if c == "'":
            mode = "char"
            i += 1
            continue
        if c == "\\":
            problems.append((line, "بک‌اسلش بیرون از رشته — احتمالاً کوتیشن تودرتو"))
            i += 1
            continue
        if tdepth:
            if c == "{":
                tdepth[-1] += 1
            elif c == "}":
                tdepth[-1] -= 1
                if tdepth[-1] == 0:
                    tdepth.pop()
                    mode = "str"
        i += 1

    if mode == "str":
        problems.append((line, "رشتهٔ بسته‌نشده تا انتهای فایل"))
    if mode == "raw":
        problems.append((line, "رشتهٔ خام بسته‌نشده تا انتهای فایل"))
    return problems


def main(argv):
    targets = argv[1:] or ["app/src"]
    files, bad = 0, []
    for target in targets:
        p = pathlib.Path(target)
        paths = [p] if p.is_file() else sorted(p.rglob("*.kt"))
        for f in paths:
            files += 1
            for line, why in scan(f.read_text(encoding="utf-8"), str(f)):
                bad.append((str(f), line, why))
    if bad:
        print("check_kotlin_quotes: ❌ %d مورد" % len(bad))
        for f, line, why in bad[:40]:
            print("   %s:%s: %s" % (f, line, why))
        return 1
    print("check_kotlin_quotes: سالم — %d فایل، همهٔ رشته‌ها بسته‌اند" % files)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
