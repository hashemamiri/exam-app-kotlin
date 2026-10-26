#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.32 — دنبالهٔ گریز نامجاز در رشته‌های کوتلین.

درسِ V335.32: در یک رشتهٔ معمولی (نه raw) نوشتن `\\{` یا `\\d` کامپایل را می‌شکند
(`Unsupported escape sequence`) و فقط در CI دیده می‌شد. این اسکریپت همان کلاس خطا را
پیش از ارسال پیدا می‌کند: رشته‌های سه‌کوتیشن (raw) و کامنت‌ها نادیده گرفته می‌شوند.

کاربرد:  python3 scripts/check_kotlin_escapes.py [مسیر...]
"""
import pathlib
import sys

ALLOWED = set("tbnr'\"\\$u")  # دنباله‌های مجاز کوتلین + \u


def scan(text: str, name: str):
    problems = []
    i, n, line = 0, len(text), 1
    state = "code"  # code | line_comment | block_comment | string | raw | char
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if ch == "\n":
            line += 1
            if state == "line_comment":
                state = "code"
        if state == "code":
            if ch == "/" and nxt == "/":
                state = "line_comment"; i += 2; continue
            if ch == "/" and nxt == "*":
                state = "block_comment"; i += 2; continue
            if text.startswith('"""', i):
                state = "raw"; i += 3; continue
            if ch == '"':
                state = "string"; i += 1; continue
            if ch == "'":
                state = "char"; i += 1; continue
        elif state == "block_comment":
            if ch == "*" and nxt == "/":
                state = "code"; i += 2; continue
        elif state == "raw":
            if text.startswith('"""', i):
                # """ پایانی؛ اگر """ """ پشت سر هم باشد ("""..."""" ) هم درست کار می‌کند
                state = "code"; i += 3; continue
        elif state in ("string", "char"):
            if ch == "\\":
                esc = nxt
                if esc and esc not in ALLOWED:
                    problems.append((line, "\\" + esc))
                i += 2
                continue
            if state == "string" and ch == '"':
                state = "code"
            elif state == "char" and ch == "'":
                state = "code"
        i += 1
    return problems


def main(argv):
    targets = argv[1:] or ["app/src"]
    files, bad = 0, []
    for target in targets:
        p = pathlib.Path(target)
        paths = [p] if p.is_file() else sorted(p.rglob("*.kt"))
        for f in paths:
            files += 1
            for line, esc in scan(f.read_text(encoding="utf-8"), str(f)):
                bad.append(f"{f}:{line}: دنبالهٔ گریز نامجاز {esc!r}")
    if bad:
        print("check_kotlin_escapes: ❌ %d مورد" % len(bad))
        for b in bad[:40]:
            print("   " + b)
        return 1
    print("check_kotlin_escapes: سالم — %d فایل" % files)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
