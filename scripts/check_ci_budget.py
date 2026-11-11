#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V362.1 — نگهبان «بودجهٔ دقیقهٔ Actions» پس از خصوصی‌شدن ریپو.

چرا این فایل هست:
    ریپوی `exam-app-kotlin` از عمومی به خصوصی رفت. در ریپوی عمومی دقیقهٔ
    Actions نامحدود و رایگان است؛ در ریپوی خصوصی پلن رایگان فقط
    **۲٬۰۰۰ دقیقه در ماه** می‌دهد.

    اندازه‌گیری واقعی ۳۰ روز پیش از این تغییر (git log، بدون merge):
      • ~۲۷۷ push که کد اپ را لمس می‌کرد × ~۱۵ دقیقه ≈ ۴٬۱۵۰ دقیقه
      • ~۱۹۶ push که سایت را لمس می‌کرد × ~۱۳ دقیقه ≈ ۲٬۵۵۰ دقیقه
      • جمع ≈ ۶٬۷۰۰ دقیقه — بیش از سه برابر سهمیه.

    دو سیاست تازه اضافه شد و این نگهبان آن‌ها را پین می‌کند تا پچ‌های بعدی
    بی‌خبر «هر push = یک بیلد» را برنگردانند:

      ۱) در android.yml خودِ job قفل است: فقط push روی شاخهٔ staging
         (نسخهٔ آزمایشی) یا اجرای دستی Run workflow. اجرای نسخهٔ اصلی:
         Actions → «آزمون آنلاین» → Run workflow (شاخهٔ main).
      ۲) در site.yml دروازهٔ مرورگری (نصب Playwright + آزمون سرتاسری +
         گالری تصاویر) پیش‌فرض خاموش است و با Variables → E2E_GATE=on
         یا اجرای دستی روشن می‌شود. انتشار سایت هیچ‌وقت بلاک نمی‌شود.

    هر دو گردش‌کار سقف زمان هم دارند تا اجرای گیرکرده سهمیه را نخورد.

اجرا:  python3 scripts/check_ci_budget.py
"""
from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"

APP_LOCK = "if: github.event_name == 'workflow_dispatch' || github.ref_name == 'staging'"
E2E_GATE = "vars.E2E_GATE == 'on' || github.event_name == 'workflow_dispatch'"

fails: list[str] = []


def ok(name: str, cond: bool, extra: str = "") -> None:
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + extra) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def main() -> int:
    android = (WF / "android.yml").read_text(encoding="utf-8")
    site = (WF / "site.yml").read_text(encoding="utf-8")

    print("=== ۱) بیلد اپ: فقط staging خودکار، انتشار اصلی دستی ===")
    ok("job اپ به staging/دستی قفل است", APP_LOCK in android)
    ok("اجرای دستی برای انتشار اصلی هست", "workflow_dispatch:" in android)
    # پین‌های قدیمی نباید بی‌خبر حذف شوند
    ok("شاخه‌ها همان main+staging مانده‌اند", "branches: [main, staging]" in android)
    ok("قاعدهٔ '!site/**' دست‌نخورده مانده", "'!site/**'" in android)
    ok("نسخه‌بندی APP_VERSION هنوز CI را صدا می‌زند", "!text/APP_VERSION.txt" not in android)
    ok("سقف زمان job اپ تعیین شده", "timeout-minutes: 45" in android)
    ok("نگهبان بودجه در زنجیرهٔ نگهبان‌های اپ هست", "check_ci_budget" in android)

    print("\n=== ۲) سایت: انتشار خودکار بماند، دروازهٔ مرورگری اختیاری شود ===")
    ok("سایت هنوز روی main و staging خودکار منتشر می‌شود", "branches: [main, staging]" in site)
    ok("دروازهٔ E2E با E2E_GATE/workflow_dispatch قفل است", E2E_GATE in site)
    ok("نصب Playwright هم قفل همان دروازه است",
       "name: Install Playwright & Chromium for visual UI gate\n        if: " + E2E_GATE in site)
    ok("آزمون سرتاسری هم قفل همان دروازه است",
       "name: Run automated browser visual & E2E gate on site_out\n        if: " + E2E_GATE in site)
    ok("سقف زمان job سایت تعیین شده", "timeout-minutes: 30" in site)
    ok("سقف آرتیفکت (یک روز) دست‌نخورده مانده", "retention-days: 1" in site)
    ok("گزارش هنوز در Step Summary می‌رود", "GITHUB_STEP_SUMMARY" in site)
    ok("نگهبان بودجه در گام‌های سایت هست", "scripts/check_ci_budget.py" in site)

    print("\n=== ۳) نگهبان‌های پایتونیِ خالص همیشه اجرا می‌شوند ===")
    for name in ("check_workflow_scripts.py", "check_actions_storage.py"):
        ok(f"{name} در سایت هست", name in site)
    for name in ("check_workflow_scripts", "check_actions_storage"):
        ok(f"{name} در حلقهٔ اپ هست", name in android)

    print("\n=== ۴) مسیرهای ابزاری از قاعدهٔ بالا رفتن نسخه مستثنا بمانند ===")
    bump = (ROOT / "scripts" / "check_version_bump.py").read_text(encoding="utf-8")
    ok("ابزارها مستثنا شده‌اند", 'TOOLING_PREFIXES = (".github/", "scripts/")' in bump)
    ok("سایت و مستندات هنوز مستثنا هستند", '("site/", "docs/", "relay/")' in bump)

    print()
    if fails:
        print(f"CI_BUDGET: FAIL ({len(fails)} مورد)")
        print("   علت معمول: یک پچ تازه «هر push = یک بیلد» را برگردانده، گام")
        print("   دروازهٔ مرورگری را از قفل E2E_GATE بیرون آورده، یا سقف زمان را")
        print("   برداشته است. مستند: هندآف بخش ۳۴۱.")
        return 1
    print("CI_BUDGET: PASS — سیاست بودجهٔ دقیقه سالم است")
    return 0


if __name__ == "__main__":
    sys.exit(main())
