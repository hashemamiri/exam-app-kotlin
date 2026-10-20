#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""بررسی ساختاری فایل‌های Kotlin: آکولادهای باز/بسته باید صفر شود.

این کنترل همان اشتباهی را می‌گیرد که کامپایلر محلی (بدون AndroidX) نمی‌گیرد و تنها در
CI دیده می‌شد: «Expecting '}'» یا افتادن یک تابع سطح‌بالا داخل تابع دیگر
(«Modifier 'private' is not applicable to 'local function'»).

نکتهٔ مهم: قالب رشته‌ای `${…}` می‌تواند خودش رشته داشته باشد
(مثل `${if (x) "a" else "b"}`)؛ اسکنر ساده در همین جا اشتباه می‌کرد و فایل‌های سالم را
معیوب گزارش می‌داد. این نسخه با پشتهٔ وضعیت (کد/رشته/رشتهٔ خام/قالب) کار می‌کند.
"""
from __future__ import annotations

import io
import sys
from pathlib import Path

CODE = "code"
STR = "str"
RAW = "raw"


def frame(depth: int = 0) -> dict:
    return {"kind": CODE, "depth": depth, "string": ""}


def scan(text: str, path: str) -> list[str]:
    problems: list[str] = []
    stack = [frame()]
    i = 0
    n = len(text)
    line = 1

    def top() -> dict:
        return stack[-1]

    while i < n:
        ch = text[i]
        f = top()

        if f["kind"] == CODE:
            if text.startswith("//", i):
                j = text.find("\n", i)
                i = n if j < 0 else j
                continue
            if text.startswith("/*", i):
                j = text.find("*/", i + 2)
                if j < 0:
                    problems.append("%s:%d: کامنت بلوکی بسته نشده" % (path, line))
                    return problems
                line += text.count("\n", i, j)
                i = j + 2
                continue
            if text.startswith('"""', i):
                stack.append({"kind": RAW})
                i += 3
                continue
            if ch == '"':
                stack.append({"kind": STR})
                i += 1
                continue
            if ch == "`":
                # نام تابع با بک‌تیک (تست‌ها) — می‌تواند آپاستروف داشته باشد؛ تا بک‌تیک بعدی رد شود
                j = text.find("`", i + 1)
                if j < 0:
                    problems.append("%s:%d: بک‌تیک بسته نشده" % (path, line))
                    return problems
                line += text.count("\n", i, j)
                i = j + 1
                continue
            if ch == "'":
                # کاراکتر literal: '{' یا '}' یا '\'' — یک واحد است
                i += 1
                while i < n:
                    if text[i] == "\\":
                        i += 2
                        continue
                    if text[i] == "'":
                        i += 1
                        break
                    if text[i] == "\n":
                        break
                    i += 1
                continue
            if ch == "{":
                f["depth"] += 1
                i += 1
                continue
            if ch == "}":
                if f["depth"] == 0:
                    # پایان یک قالب ${…} یا آکولاد اضافی
                    if len(stack) > 1 and stack[-2]["kind"] in (STR, RAW):
                        stack.pop()
                        i += 1
                        continue
                    problems.append("%s:%d: آکولاد بستهٔ اضافی" % (path, line))
                    return problems
                f["depth"] -= 1
                i += 1
                continue
            if ch == "\n":
                line += 1
            i += 1
            continue

        # داخل رشته (معمولی یا خام)
        if f["kind"] == STR and ch == "\\":
            i += 2
            continue
        if f["kind"] == STR and text.startswith("${", i):
            stack.append(frame(0))
            i += 2
            continue
        if f["kind"] == RAW and text.startswith("${", i):
            stack.append(frame(0))
            i += 2
            continue
        if f["kind"] == RAW and ch == '"':
            # در رشتهٔ خام، هر دستهٔ سه‌قلاپی یا بیشتر پایان رشته است (سهٔ آخر نشانه، بقیه محتوا)
            k = i
            while k < n and text[k] == '"':
                k += 1
            run = k - i
            if run >= 3:
                stack.pop()
                line += 0
                i = k
                continue
            i = k
            continue
        if f["kind"] == STR and ch == '"':
            stack.pop()
            i += 1
            continue
        if ch == "\n":
            line += 1
            if f["kind"] == STR:
                # سهل‌گیرانه: رشتهٔ ناتمام را در پایان خط می‌بندیم تا اسکن ادامه یابد
                stack.pop()
        i += 1

    if len(stack) != 1:
        kinds = "/".join(s["kind"] for s in stack[1:])
        problems.append("%s: %d بخش باز مانده (%s) — احتمالاً رشته یا قالب بسته نشده" % (path, len(stack) - 1, kinds))
        return problems
    if stack[0]["depth"] != 0:
        d = stack[0]["depth"]
        problems.append(
            "%s: آکولاد نامتوازن — %d آکولاد %s مانده (CI: Expecting '}')"
            % (path, abs(d), "باز" if d > 0 else "بستهٔ اضافی")
        )
    return problems


def main() -> int:
    roots = sys.argv[1:] or ["app/src/main/java", "app/src/test/java"]
    files: list[Path] = []
    for r in roots:
        files.extend(sorted(Path(r).rglob("*.kt")))
    bad: list[str] = []
    for f in files:
        bad.extend(scan(io.open(f, encoding="utf-8").read(), str(f)))
    if bad:
        for b in bad:
            print("❌ " + b)
        print("check_kotlin_braces: %d problem(s) / %d files" % (len(bad), len(files)))
        return 1
    print("check_kotlin_braces: سالم — %d فایل" % len(files))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
