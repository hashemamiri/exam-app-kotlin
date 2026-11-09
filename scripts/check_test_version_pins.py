#!/usr/bin/env python3
"""V356.1/V357 — نگهبان پین‌های شکنندهٔ نسخه در آزمون‌ها.

چرا لازم شد: در V354 عدد نسخه در آزمون سخت‌رمز شده بود و انتشار بعدی
آن را شکست. همان اشتباه در آزمون V355 تکرار شد و CI بستهٔ V356 را قرمز
کرد (۱۶۵ آزمون سبز، ۱ شکست) در حالی که خودِ اپ سالم بود.

قاعده: هیچ آزمونی نباید شمارهٔ نسخهٔ اپ را به‌صورت ثابت با فایل
APP_VERSION.txt یا با appVersion بلوک بالای فهرست تغییرات مقایسه کند.
چیزی که باید سنجیده شود «هم‌خوانی» است، نه یک عدد خاص.

مجاز: assertEquals("۱.۰۳.۷۱", PersianDigits.convert("1.03.71")) — این
آزمون تبدیل رقم است و به انتشار گره نخورده.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TESTS = ROOT / "app" / "src" / "test" / "java"

# الگوهای ممنوع: مقایسهٔ عدد نسخهٔ ثابت با منبع زندهٔ نسخه
BANNED = [
    (re.compile(r'assertEquals\(\s*"\d+\.\d+\.\d+"\s*,\s*src\(\s*"text/APP_VERSION\.txt"'),
     'شمارهٔ نسخه را با فایل APP_VERSION سخت‌رمز نکنید'),
    (re.compile(r'assertEquals\(\s*"\d+\.\d+\.\d+"\s*,\s*top\.appVersion'),
     'شمارهٔ نسخه را با appVersion بلوک بالا سخت‌رمز نکنید'),
    (re.compile(r'assertEquals\(\s*"V\d+(?:\.\d+)?"\s*,\s*top\.version'),
     'نام نسخهٔ بلوک بالا را سخت‌رمز نکنید'),
    # V357 — این شکل سه بار تکرار شد: محتوای «بلوک بالا» پین می‌شد و
    # هر انتشار بعدی آن را می‌شکست. محتوا باید به بلوک نسخهٔ خودش گره
    # بخورد، نه به بلوک بالا که با هر انتشار جابه‌جا می‌شود.
    # فقط «حضور یک رشتهٔ مشخص» ممنوع است. سنجهٔ قاعدهٔ همیشگی مثل
    # assertFalse(...contains(Regex(...))) باید مجاز بماند، چون آن
    # قاعده با هر انتشار برقرار است و پین محتوا نیست.
    (re.compile(r'top\.bullets[^\n]*\.contains\(\s*"'),
     'محتوای بلوک بالا را پین نکنید؛ به بلوک نسخهٔ خودش گره بزنید'),
]


def main() -> int:
    if not TESTS.exists():
        print("check_test_version_pins: پوشهٔ آزمون پیدا نشد")
        return 1
    problems: list[str] = []
    scanned = 0
    for path in sorted(TESTS.rglob("*.kt")):
        scanned += 1
        text = path.read_text(encoding="utf-8")
        for lineno, line in enumerate(text.split("\n"), start=1):
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            for pattern, why in BANNED:
                if pattern.search(line):
                    rel = path.relative_to(ROOT)
                    problems.append(f"{rel}:{lineno} — {why}\n    {stripped}")
    if problems:
        print("check_test_version_pins: FAIL")
        for p in problems:
            print("  ✗ " + p)
        print("\nراه درست: نسخه را از فایل بخوانید و با appVersion مقایسه کنید:")
        print('  val version = src("text/APP_VERSION.txt").trim()')
        print('  assertEquals(version, top.appVersion)')
        return 1
    print(f"check_test_version_pins: سالم — {scanned} فایل آزمون بررسی شد")
    return 0


if __name__ == "__main__":
    sys.exit(main())
