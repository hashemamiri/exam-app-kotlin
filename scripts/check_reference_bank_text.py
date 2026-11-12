#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V363 — نگهبان «متن بانک مرجع در همهٔ پایه‌ها دیده شود».

چرا: شاردهای پایه‌های ۶، ۷ و ۸ فیلد text را خالی گذاشته‌اند و متن کامل
فقط در q_html است. فهرست بانک و سؤالِ افزوده‌شده به آزمون همان text را
می‌خواندند و بی‌متن می‌شدند، ولی پیش‌نمایش (q_html) سالم بود.

این نگهبان سه چیز را پین می‌کند تا پچ‌های بعدی پل را حذف نکنند:
  ۱) اپ: ReferenceBankMedia.displayText + استفاده‌اش در مپر؛
  ۲) سایت: draft از `p.text || plain(p.q_html)` می‌خواند؛
  ۳) ابزار ترمیم شارد (scripts/repair_reference_bank_text.py) سر جایش است
     و کلید نسخهٔ شارد بالا رفته (v3710) تا catalog کهنه از کش نیاید.

اجرا:  python3 scripts/check_reference_bank_text.py
"""
from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODELS = ROOT / "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt"
MAPPER = ROOT / "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt"
DIALOG = ROOT / "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt"
SITE = ROOT / "site/src/refbank.js"
TOOL = ROOT / "scripts/repair_reference_bank_text.py"
ANDROID = ROOT / ".github/workflows/android.yml"
SITE_WF = ROOT / ".github/workflows/site.yml"

fails: list[str] = []


def ok(name: str, cond: bool, extra: str = "") -> None:
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def read(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def main() -> int:
    models = read(MODELS)
    mapper = read(MAPPER)
    dialog = read(DIALOG)
    site = read(SITE)

    print("=== ۱) اپ: پل متن از q_html ===")
    ok("مدل تابع displayText دارد", "fun displayText(text: String, qHtml: String, limit: Int = 0)" in models)
    ok("متن ساده بدون شکستن نیم‌فاصله", "fun htmlToText(html: String): String" in models)
    ok("نیم‌فاصله حفظ می‌شود", 'replace("&zwnj;", "\\u200c")' in models)
    ok("مپر از پل استفاده می‌کند، «property نمایشی» هم همان پل را صدا می‌زند",
       "ReferenceBankMedia.displayText(text, qHtml)" in models
       and "get() = ReferenceBankMedia.displayText(text, qHtml)" in models
       and "mathJaxToTokens(displayText)" in mapper)
    ok("مپر دیگر فقط text را نمی‌خواند",
       "val textTokens = ReferenceBankFormula.mathJaxToTokens(text)" not in mapper)
    ok("گزینه‌های کاملاً خالی تستی حساب نمی‌شوند", "optionsBlank && !hasOptionImages" in mapper)
    ok("فرمول باز در اپ بسته می‌شود", "fun balanceMath(value: String): String" in models)
    ok("برش فهرست اپ از برش امن می‌گذرد",
       'ReferenceBankMedia.displayText(row.text, "", 180)' in dialog)
    ok("سایت هم برش امن دارد", "function safeSnippet(value, limit)" in site)
    ok("فهرست اپ برای سطر بی‌متن راهنما می‌دهد", "در فهرست این پایه نیست" in dialog)

    print("\n=== ۲) سایت: همان پل در ساخت سؤال آزمون ===")
    ok("سایت متن را از q_html می‌گیرد", "p.text || plainQuestion(p.q_html || '')" in site)
    ok("سایت نیم‌فاصله را نمی‌شکند", "function plainQuestion(html)" in site)
    ok("فهرست سایت برای سطر بی‌متن راهنما می‌دهد", "در فهرست این پایه نیست" in site)
    ok("سایت کلید نسخهٔ تازه دارد", "SHARD_VERSION = 'v3710'" in site)
    ok("اپ کلید نسخهٔ تازه دارد", 'SHARD_VERSION = "v3710"' in models)
    ok("نشانی‌ها نسخه‌دار می‌مانند", "ReferenceBankContract.shardUrl(path)" in
       read(ROOT / "app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt"))

    print("\n=== ۳) ابزار ترمیم شارد ===")
    tool = read(TOOL)
    ok("ابزار ترمیم موجود است", TOOL.is_file())
    ok("حالت گزارش دارد", '"--check"' in tool)
    ok("شارد سؤال اختیاری است", '"--with-questions"' in tool)
    ok("حالت پوشهٔ محلی دارد (برای شاردی که روی سرور نیست)", '"--local-dir"' in tool)
    ok("حالت سبک «فقط فهرست» دارد", '"--only-catalog"' in tool)
    ok("فرمول نیمه‌بریده را می‌بندد", "def balance_math(value)" in tool)
    ok("برش امن دارد", "def safe_snippet(value, limit)" in tool)
    ok("متن را از q_html می‌سازد", "q_html" in tool)
    ok("نیم‌فاصله را در پایتون هم حفظ می‌کند",
       '"&zwnj;": "\\u200c"' in tool or '"&zwnj;", "\\u200c"' in tool)
    uploader = read(ROOT / "scripts/upload_reference_bank.py")
    ok("ابزار بارگذاری موجود است", bool(uploader))
    ok("بارگذار پیشوند پوشه را هم می‌پذیرد (--only catalog)",
       'startswith(wanted + "/")' in uploader)

    print("\n=== ۴) اتصال به گردش‌کار ===")
    android = read(ANDROID)
    site_wf = read(SITE_WF)
    ok("نگهبان در حلقهٔ نگهبان‌های اپ هست", "check_reference_bank_text" in android)
    ok("نگهبان در گام‌های سایت هست", "check_reference_bank_text.py" in site_wf)

    print("\n=== ۵) نگهبان‌های قبلی دست‌نخورده ===")
    for name in ("check_reference_bank_formula.py", "check_reference_bank_option_images.py",
                 "check_reference_bank_bundle.py"):
        ok(name + " سالم است", (ROOT / "scripts" / name).is_file())

    print()
    if fails:
        print(f"REFERENCE_BANK_TEXT: FAIL ({len(fails)} مورد)")
        print("   علت معمول: پچی پل «text ← q_html» را حذف کرده یا کلید نسخهٔ")
        print("   شارد را برگردانده است. مستند: هندآف بخش ۳۴۲.")
        return 1
    print("REFERENCE_BANK_TEXT: PASS — متن بانک مرجع در اپ و سایت از هر دو مسیر می‌آید")
    return 0


if __name__ == "__main__":
    sys.exit(main())
