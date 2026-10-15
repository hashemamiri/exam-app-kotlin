#!/usr/bin/env python3
"""V298 — اعمال نسخهٔ آزمایشی (شاخهٔ staging) روی نسخهٔ اصلی (شاخهٔ main).

اجرا در پوشهٔ پروژه (WSL):
    python3 scripts/promote.py            → اعمال همهٔ نسخه‌های تأییدشدهٔ آزمایشی روی اپ و سایت اصلی
    python3 scripts/promote.py --status   → چه نسخه‌هایی در آزمایشی هست که هنوز اصلی نشده؟
    python3 scripts/promote.py --cancel   → انصراف: شاخهٔ آزمایشی و کد محلی به آخرین نسخهٔ اصلی برمی‌گردد

گردش کار: apply_vXX.py → شاخهٔ staging (اپ «آزمون (آزمایشی)» + https://staging.onlineexam.pages.dev)
          → بررسی و تأیید شما → promote.py → شاخهٔ main (اپ اصلی + onlineexam.ir)
"""
import subprocess
import sys


def git(*args, check=True, capture=True):
    r = subprocess.run(["git", *args], text=True, capture_output=capture)
    if check and r.returncode != 0:
        print("❌ خطا در دستور: git " + " ".join(args))
        print((r.stderr or r.stdout or "").strip())
        sys.exit(1)
    return (r.stdout or "").strip()


def rev(name):
    return git("rev-parse", "--verify", "--quiet", name, check=False)


def pending():
    return git("log", "--oneline", "origin/main..origin/staging", check=False)


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if git("rev-parse", "--is-inside-work-tree", check=False) != "true":
        print("❌ این دستور باید داخل پوشهٔ پروژه اجرا شود:")
        print("   cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && python3 scripts/promote.py")
        sys.exit(1)
    print("⏳ دریافت وضعیت از GitHub…")
    git("fetch", "--prune", "origin")
    main_sha, stg_sha, head = rev("origin/main"), rev("origin/staging"), rev("HEAD")
    if not main_sha:
        print("❌ شاخهٔ main روی GitHub پیدا نشد.")
        sys.exit(1)

    if mode == "--status":
        p = pending()
        if not stg_sha or not p:
            print("✅ نسخهٔ آزمایشی چیزی جلوتر از نسخهٔ اصلی ندارد.")
        else:
            print("🧪 در انتظار تأیید (آزمایشی ولی هنوز اصلی نشده):")
            print(p)
            print("\nاعمال: python3 scripts/promote.py      انصراف: python3 scripts/promote.py --cancel")
        return

    if mode == "--cancel":
        p = pending()
        if not stg_sha or not p:
            print("✅ چیزی برای انصراف نیست؛ آزمایشی و اصلی یکی‌اند.")
            return
        print("این نسخه‌ها از شاخهٔ آزمایشی برداشته می‌شوند:\n" + p)
        if input("ادامه؟ (بله/خیر): ").strip() not in ("بله", "y", "yes", "Y"):
            print("لغو شد.")
            return
        git("reset", "--hard", "origin/main", capture=False)
        git("push", "--force", "origin", main_sha + ":refs/heads/staging", capture=False)
        print("\n↩️ انجام شد. کد محلی و شاخهٔ آزمایشی به نسخهٔ اصلی برگشتند.")
        print("⚠️ به دستیار بگویید کدام نسخه لغو شد تا بستهٔ بعدی بر پایهٔ نسخهٔ اصلی ساخته شود.")
        return

    if mode:
        print("گزینهٔ ناشناخته: " + mode + "\n" + __doc__)
        sys.exit(1)

    if not stg_sha:
        print("❌ شاخهٔ آزمایشی (staging) هنوز وجود ندارد؛ اول یک apply_vXX.py اجرا کنید.")
        sys.exit(1)
    p = pending()
    if not p:
        print("✅ چیزی برای اعمال نیست؛ نسخهٔ اصلی همین حالا برابر آزمایشی است.")
        return
    if head != stg_sha:
        print("❌ کد محلی با شاخهٔ آزمایشی روی GitHub یکی نیست.")
        print("   محلی: " + head[:8] + "   آزمایشی: " + stg_sha[:8])
        print("   اگر آخرین apply_vXX.py با خطا تمام شده، اول آن را درست اجرا کنید؛ یا با --cancel همه را به نسخهٔ اصلی برگردانید.")
        sys.exit(1)
    if subprocess.run(["git", "merge-base", "--is-ancestor", main_sha, stg_sha]).returncode != 0:
        print("❌ شاخهٔ اصلی تغییری دارد که در آزمایشی نیست (احتمالاً ویرایش مستقیم روی GitHub). با دستیار هماهنگ کنید.")
        sys.exit(1)
    print("این نسخه‌ها روی اپ و سایت اصلی اعمال می‌شوند:\n" + p)
    if input("تأیید می‌کنید؟ (بله/خیر): ").strip() not in ("بله", "y", "yes", "Y"):
        print("لغو شد؛ چیزی تغییر نکرد.")
        return
    git("push", "origin", stg_sha + ":refs/heads/main", capture=False)
    print("\n✅ اعمال شد. GitHub Actions اپ اصلی را منتشر می‌کند و onlineexam.ir به‌روز می‌شود (چند دقیقه).")


if __name__ == "__main__":
    main()
