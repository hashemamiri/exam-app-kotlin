#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""بستهٔ نصب خودکار — اجرا در پوشهٔ پروژه:
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && python3 /mnt/c/Users/Hashem/Downloads/apply_vXX.py

V335.39 — دو ایمنی تازه (درس‌های همین نسخه):
  • save_dirty_tree: اگر درخت کاری تغییرِ commit‌نشده داشت (مثل پیش‌نویس V335.36 در
    نصب اول)، کار در یک شاخهٔ wip-auto-… ذخیره می‌شود و نصب ادامه می‌یابد؛
    پیش‌تر نصاب در «git checkout -B main origin/main» می‌شکست و هیچ کاری انجام نمی‌شد.
  • push_checked: اگر توکن گیت مجوز workflow نداشته باشد، گیت کل push را رد می‌کند؛
    اینجا فقط تغییر site.yml از کامیت برداشته و نسخهٔ تازه کنار نصاب ذخیره می‌شود
    تا بقیهٔ نسخه از دست نرود.
"""
import base64, gzip, io, os, shutil, subprocess, sys, tarfile, time

VERSION = "VXX"
TARGET = "staging"  # V298.3 — «staging» = فقط نسخهٔ آزمایشی؛ «main» = اصلی (و آزمایشی هم همگام می‌شود)
COMMIT_MSG = "..."
FILES = []
REMOVE = []
PAYLOAD = """"""

WF_PATH = ".github/workflows/site.yml"


def run(cmd, check=True):
    print("$ " + " ".join(cmd), flush=True)
    r = subprocess.run(cmd)
    if check and r.returncode != 0:
        print("\n❌ خطا در اجرای دستور بالا — کار متوقف شد (چیزی commit نشد).")
        sys.exit(r.returncode)
    return r.returncode


def git_ident():
    return ["-c", "user.name=Hashem Amiri", "-c", "user.email=hashemamiri@users.noreply.github.com"]


def save_dirty_tree():
    """تغییرات commit‌نشده را در شاخهٔ wip-auto-… ذخیره می‌کند تا نصب ادامه یابد."""
    st = subprocess.run(["git", "status", "--porcelain"], text=True, capture_output=True).stdout.strip()
    if not st:
        return None
    lines = st.splitlines()
    stamp = time.strftime("%Y%m%d-%H%M%S")
    branch = "wip-auto-" + VERSION.lower().replace(".", "_") + "-" + stamp
    print("\n⚠ درخت کاری " + str(len(lines)) + " تغییر commit‌نشده دارد (نصب با reset --hard آن‌ها را پاک می‌کرد).")
    print("  تغییرات در شاخهٔ «" + branch + "» ذخیره می‌شود و نصب ادامه پیدا می‌کند…")
    run(["git", "checkout", "-B", branch])
    run(["git", "add", "-A"])
    r = subprocess.run(["git"] + git_ident() + ["commit", "-m", "WIP خودکار پیش از " + VERSION + " (تغییرات commit‌نشده)"])
    if r.returncode != 0:
        print("❌ ذخیرهٔ تغییرات ناتمام ناموفق بود؛ برای جلوگیری از از‌دست‌رفتن کار متوقف شد.")
        print("   دستی انجام دهید:  git checkout -b wip-دستی && git add -A && git commit -m WIP")
        sys.exit(1)
    print("✅ کار ناتمام ذخیره شد: git checkout " + branch + "   (این شاخه push نمی‌شود)")
    print("   مقایسه با نسخهٔ اصلی: git diff --stat " + branch + " origin/main")
    return branch


def push_checked(refspec, check=True):
    """push با تشخیص خطای «توکن اجازهٔ تغییر workflow ندارد»؛ بقیهٔ نسخه از دست نمی‌رود."""
    r = subprocess.run(["git", "push", "origin", refspec], text=True, capture_output=True)
    out = ((r.stdout or "") + (r.stderr or "")).strip()
    if out:
        print(out, flush=True)
    if r.returncode == 0:
        return 0
    low = out.lower()
    if ("workflow" in low or "refusing" in low) and os.path.isfile(WF_PATH):
        pend = os.path.join(os.path.dirname(os.path.abspath(__file__)), "site_" + VERSION.replace(".", "_").lower() + "_workflow.yml.pending")
        shutil.copyfile(WF_PATH, pend)
        print("\n⚠ توکن گیت اجازهٔ تغییر فایل workflow را ندارد (scope=workflow لازم است).")
        print("  نسخهٔ تازهٔ site.yml اینجا ذخیره شد: " + pend)
        print("  تغییر workflow از کامیت برداشته می‌شود و بقیهٔ نسخه دوباره push می‌شود…")
        run(["git", "checkout", "HEAD~1", "--", WF_PATH], check=False)
        run(["git"] + git_ident() + ["commit", "--amend", "--no-edit"], check=False)
        r2 = subprocess.run(["git", "push", "origin", refspec], text=True, capture_output=True)
        out2 = ((r2.stdout or "") + (r2.stderr or "")).strip()
        if out2:
            print(out2, flush=True)
        if r2.returncode == 0:
            print("\n📌 بقیهٔ نسخه اعمال شد. برای فعال‌کردن هم‌گام‌سازی خودکار، قطعهٔ")
            print("   docs/ci/site_arvan_sync_step.yml را در GitHub → Actions → site.yml جای‌گذاری کنید")
            print("   (یا توکنی با scope=workflow بسازید و همین نصاب را دوباره اجرا کنید).")
            return 0
        return r2.returncode
    if check:
        print("\n❌ push ناموفق بود (شبکه/دسترسی/تعارض).")
        sys.exit(r.returncode)
    return r.returncode


def main():
    if not (os.path.isdir(".git") and os.path.isfile("scripts/verify_native_final.py")):
        print("❌ این دستور را باید داخل پوشهٔ پروژهٔ exam-app-kotlin اجرا کنید:")
        print("   cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin")
        sys.exit(1)
    print("نصب " + VERSION + " ...")
    # پاک‌سازی هرگونه وضعیت نیمه‌کاره قبلی (rebase/merge) و همگام‌سازی کامل با ریموت
    run(["git", "rebase", "--abort"], check=False)
    run(["git", "merge", "--abort"], check=False)
    save_dirty_tree()
    run(["git", "fetch", "origin"])
    run(["git", "checkout", "-B", "main", "origin/main"])
    run(["git", "reset", "--hard", "origin/main"])
    data = gzip.decompress(base64.b64decode(PAYLOAD))
    with tarfile.open(fileobj=io.BytesIO(data), mode="r") as t:
        names = sorted(m.name for m in t.getmembers() if m.isfile())
        if names != sorted(FILES):
            print("❌ محتوای بسته با فهرست فایل‌ها نمی‌خواند — بسته خراب است.")
            sys.exit(1)
        for m in t.getmembers():
            if not m.isfile():
                continue
            d = os.path.dirname(m.name)
            if d:
                os.makedirs(d, exist_ok=True)
            with open(m.name, "wb") as f:
                f.write(t.extractfile(m).read())
            print("  ✓ " + m.name)
    for p in REMOVE:
        if os.path.exists(p):
            os.remove(p)
            print("  ✗ حذف " + p)
    run([sys.executable, "scripts/verify_native_final.py"])
    run([sys.executable, "scripts/check_site_sync.py"], check=False)  # V335.39 — نگهبان سایت/دامنه (خطا نمی‌دهد)
    run(["git", "add", "-A"])
    run(["git", "diff", "--cached", "--check"])
    try:
        os.remove(os.path.abspath(__file__))
    except OSError:
        pass
    run(["git"] + git_ident() + ["commit", "-m", COMMIT_MSG])
    # V298 — مقصد طبق انتخاب کاربر در گفتگو (قاعده: هر بار پرسیده می‌شود: آزمایشی / اصلی / هر دو)
    run(["git", "config", "http.postBuffer", "524288000"], check=False)
    run(["git", "config", "http.lowSpeedLimit", "0"], check=False)
    run(["git", "config", "http.lowSpeedTime", "999999"], check=False)
    if TARGET == "main":
        push_checked("HEAD:refs/heads/main")
        time.sleep(2)
        r_st = push_checked("HEAD:refs/heads/staging", check=False)  # آزمایشی هم همگام می‌شود
        if r_st != 0:
            time.sleep(3)
            push_checked("HEAD:refs/heads/staging", check=False)
        print("\n✅ " + VERSION + " نصب و روی نسخهٔ اصلی (و آزمایشی) اعمال شد. GitHub Actions اپ اصلی/سایت onlineexam.ir را به‌روز می‌کند.")
    else:
        push_checked("HEAD:refs/heads/staging")
        print("\n✅ " + VERSION + " نصب و به شاخهٔ آزمایشی (staging) ارسال شد.")
        print("   GitHub Actions نسخهٔ آزمایشی را می‌سازد: اپ «آزمون (آزمایشی)» + سایت آزمایشی https://staging.onlineexam-1q1.pages.dev")
        print("   پس از بررسی و تأیید:  python3 scripts/promote.py      (انصراف:  python3 scripts/promote.py --cancel)")


if __name__ == "__main__":
    main()
