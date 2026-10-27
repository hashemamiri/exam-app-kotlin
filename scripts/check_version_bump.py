#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.40 — نگهبان «شمارهٔ نسخه پیش از push».

قاعدهٔ V210 در `.github/workflows/android.yml`: Supabase هر `version_code` را فقط
یک بار می‌پذیرد؛ اگر به `main` push شود و `text/APP_VERSION.txt` بالا نرفته باشد،
گام «بررسی نسخه» می‌شکند و هیچ APK منتشر نمی‌شود.

این اتفاق دقیقاً در V335.39 افتاد: نسخهٔ فقط‌سایتی بود، شماره 1.03.19 همان
نسخهٔ منتشرشدهٔ V335.38 بود و job اندروید با پیام
«نسخهٔ 1.03.19 (کد 1001031900) قبلاً منتشر شده است» شکست.

این اسکریپت همان خطا را **پیش از** push می‌گیرد:
  • تغییرات (کامیت‌شده/نشده/untracked) را نسبت به `origin/main` می‌گیرد؛
  • اگر فایلی بیرون از `site/` و `docs/` تغییر کرده باشد، بالا رفتن
    `text/APP_VERSION.txt` الزامی است؛
  • قالب نسخه و هم‌خوانی‌اش با خط اول CHANGELOG را هم می‌سنجد.

اجرا:  python3 scripts/check_version_bump.py           (خروجی غیرصفر = نسخه بالا نرفته)
       python3 scripts/check_version_bump.py --strict  (هشدارها هم خطا می‌شوند)

اگر `origin/main` در دسترس نباشد (مثلاً مخزن بدون ریموت)، فقط هشدار می‌دهد و
صفر برمی‌گرداند تا اجرای آفلاین preflight نشکند.
"""
import os
import re
import subprocess
import sys

APP_VERSION = "text/APP_VERSION.txt"
CHANGELOG = "text/CHANGELOG_FA.txt"
SITE_ONLY_PREFIXES = ("site/", "docs/", "relay/")
VERSION_RE = re.compile(r"^\d{1,2}\.\d{2}\.\d{2}$")


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True)


def changed_paths(base):
    """همهٔ مسیرهای متفاوت با base: کامیت‌شده، staged، unstaged و untracked."""
    out = set()
    for args in (["diff", "--name-only", base],
                 ["diff", "--name-only", "--cached", base],
                 ["ls-files", "--others", "--exclude-standard"]):
        r = git(*args)
        if r.returncode == 0 and r.stdout.strip():
            out.update(p for p in r.stdout.splitlines() if p.strip())
    return out


def main():
    strict = "--strict" in sys.argv
    ok = True

    # ۱) قالب نسخه
    version = open(APP_VERSION, encoding="utf-8").read().strip()
    if not VERSION_RE.match(version):
        print(f"❌ قالب شمارهٔ نسخه نامعتبر است: {version!r} (قالب درست: 1.03.20)")
        ok = False

    # ۲) هم‌خوانی با خط اول CHANGELOG (تست V208 مقدار نسخه را در خط اول می‌جوید)
    first = open(CHANGELOG, encoding="utf-8").read().splitlines()[0] if os.path.isfile(CHANGELOG) else ""
    if version not in first:
        print(f"❌ خط اول CHANGELOG نسخهٔ {version} را ندارد: {first[:90]}")
        ok = False

    # ۳) قاعدهٔ بالا رفتن نسخه نسبت به origin/main
    ref = git("rev-parse", "--verify", "origin/main")
    if ref.returncode != 0:
        print("⚠ origin/main در دسترس نیست؛ قاعدهٔ بالا رفتن نسخه بررسی نشد (آفلاین).")
        return 1 if (not ok) else (0 if not strict else 0)
    paths = changed_paths("origin/main")
    if not paths:
        print("ℹ️  نسبت به origin/main تغییری وجود ندارد.")
        return 0 if ok else 1
    app_paths = sorted(p for p in paths if not p.startswith(SITE_ONLY_PREFIXES))
    # «بالا رفتن» یعنی مقدار فعلی از مقدار منتشرشده در origin/main بزرگ‌تر باشد
    # (نه فقط «فایل تغییر کرده»؛ وگرنه برگرداندن نسخه هم اشتباهاً «بالا رفته» حساب می‌شد)
    base = git("show", "origin/main:" + APP_VERSION)
    base_version = base.stdout.strip() if base.returncode == 0 else ""

    def vtuple(v):
        try:
            return tuple(int(x) for x in v.split("."))
        except ValueError:
            return None

    cur_t, base_t = vtuple(version), vtuple(base_version)
    version_bumped = bool(cur_t and base_t and cur_t > base_t)
    if app_paths and not version_bumped:
        print("❌ به main push می‌شود ولی شمارهٔ نسخه بالا نرفته است؛")
        print("   job اندروید با «این نسخه قبلاً منتشر شده است» شکست می‌خورد.")
        print("   راه‌حل:  python3 scripts/bump_app_version.py  و یک بلوک تازهٔ CHANGELOG")
        print("   فایل‌های بیرون از site/ و docs/ که تغییر کرده‌اند:")
        for p in app_paths[:12]:
            print("     • " + p)
        if len(app_paths) > 12:
            print(f"     … و {len(app_paths) - 12} فایل دیگر")
        ok = False
    elif app_paths and version_bumped:
        print(f"✅ نسخه {version} نسبت به {base_version} (origin/main) بالا رفته است.")
    elif not app_paths and version_bumped:
        print(f"✅ تغییر فقط سایت/مستندات است و نسخه هم بالا رفته ({version}).")
    else:
        print("✅ تغییر فقط سایت/مستندات است؛ بدون بالا بردن نسخه هم CI اندروید مشکلی ندارد.")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
