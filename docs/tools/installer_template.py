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
    # V341.1 — هر فایل workflow که در این کامیت عوض شده (نه فقط site.yml)؛ وگرنه
    # تغییر android.yml باعث می‌شد کل نصب با خطا متوقف شود.
    wf_changed = [
        f for f in subprocess.run(["git", "diff", "--name-only", "HEAD~1", "HEAD"],
                                  text=True, capture_output=True).stdout.split()
        if f.startswith(".github/workflows/")
    ]
    if ("workflow" in low or "refusing" in low) and wf_changed:
        here = os.path.dirname(os.path.abspath(__file__))
        pend_list = []
        for wf in wf_changed:
            if os.path.isfile(wf):
                pend = os.path.join(here, VERSION.replace(".", "_").lower() + "_" + os.path.basename(wf) + ".pending")
                shutil.copyfile(wf, pend)
                pend_list.append(pend)
        print("\n⚠ توکن گیت اجازهٔ تغییر فایل workflow را ندارد (scope=workflow لازم است).")
        for pend in pend_list:
            print("  نسخهٔ تازه اینجا ذخیره شد: " + pend)
        print("  تغییر workflow از کامیت برداشته می‌شود و بقیهٔ نسخه دوباره push می‌شود…")
        for wf in wf_changed:
            r_ck = subprocess.run(["git", "checkout", "HEAD~1", "--", wf], capture_output=True, text=True)
            if r_ck.returncode != 0:
                subprocess.run(["git", "rm", "-q", "-f", "--cached", wf], capture_output=True, text=True)
        run(["git"] + git_ident() + ["commit", "--amend", "--no-edit"], check=False)
        r2 = subprocess.run(["git", "push", "origin", refspec], text=True, capture_output=True)
        out2 = ((r2.stdout or "") + (r2.stderr or "")).strip()
        if out2:
            print(out2, flush=True)
        if r2.returncode == 0:
            print("\n📌 بقیهٔ نسخه اعمال شد. فایل(های) workflow ذخیره‌شده را دستی اعمال کنید:")
            print("   GitHub → آن فایل در .github/workflows → Edit → محتوای .pending را جای‌گذاری کنید")
            print("   (برای site.yml: قطعهٔ docs/ci/site_arvan_sync_step.yml؛")
            print("    یا توکنی با scope=workflow بسازید و همین نصاب را دوباره اجرا کنید).")
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
    # V340.2 — انتخاب پایهٔ نصب.
    # پیش‌تر نصاب همیشه روی origin/main می‌نشست؛ اگر نسخهٔ آزمایشیِ قبلی هنوز promote
    # نشده باشد (staging جلوتر از main)، کامیت تازه نوادگانِ staging نبود و push با
    # «non-fast-forward» رد می‌شد. حالا اگر staging شامل main است، پایه همان staging است.
    # V350 — این انتخاب برای «هر دو مقصد» لازم است، نه فقط آزمایشی.
    # اگر آزمایشی جلوتر از اصلی باشد و پایه origin/main بماند، کامیت تازه نوادگانِ
    # آزمایشی نیست؛ در نتیجه push به آزمایشی fast-forward نمی‌شود و نصاب به ادغام
    # سه‌طرفه می‌افتد که روی فایل‌های مشترک (سایت/نسخه) تعارض می‌دهد و آزمایشی را
    # عقب جا می‌گذارد. با نشستن روی آزمایشی، هر دو push سریع و بدون تعارض است.
    base_ref = "origin/main"
    r = subprocess.run(["git", "merge-base", "--is-ancestor", "origin/main", "origin/staging"],
                       capture_output=True)
    if r.returncode == 0:
        base_ref = "origin/staging"
    # برای مقصد اصلی، شاخهٔ کاری همان main می‌ماند حتی اگر پایه آزمایشی باشد
    # (درختِ نتیجه یکسان است؛ فقط نیای کامیت فرق می‌کند).
    local_branch = "main" if TARGET == "main" else ("staging" if base_ref == "origin/staging" else "main")
    print("پایهٔ نصب: " + base_ref)
    run(["git", "checkout", "-B", local_branch, base_ref])
    run(["git", "reset", "--hard", base_ref])
    if base_ref == "origin/staging" and local_branch != "main":
        # شاخهٔ main محلی اگر از نصب ناموفق قبلی کامیت اضافه دارد، به ریموت برگردانده می‌شود.
        # برای مقصد اصلی این کار لازم نیست (شاخهٔ جاری خودِ main است و git اجازه نمی‌دهد).
        run(["git", "branch", "-f", "main", "origin/main"], check=False)
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
    # V350 — بسته‌های سایت که دیگر در panels_keep.txt نیستند پاک می‌شوند.
    # چرا: فهرست REMOVE فقط فایل‌های «پایهٔ بسته» را می‌شناسد؛ اگر شاخهٔ مقصد
    # بسته‌های تازه‌تری داشته باشد (آزمایشی جلوتر از اصلی)، آن‌ها بی‌استفاده
    # روی دیسک می‌ماندند.panels_keep.txt مرجع یگانهٔ بسته‌های جاری است.
    keep_path = os.path.join("site", "panels_keep.txt")
    if os.path.exists(keep_path):
        with open(keep_path, encoding="utf-8") as f:
            keep = {ln.strip() for ln in f if ln.strip()}
        for name in sorted(os.listdir("site")):
            if name.startswith("panels.") and name.endswith(".js") and name not in keep:
                os.remove(os.path.join("site", name))
                print("  ✗ حذف بستهٔ کهنهٔ سایت " + name)
    run([sys.executable, "scripts/verify_native_final.py"])
    run([sys.executable, "scripts/check_site_sync.py"], check=False)  # V335.39 — نگهبان سایت/دامنه (خطا نمی‌دهد)
    run(["git", "add", "-A"])
    run(["git", "diff", "--cached", "--check"])
    try:
        os.remove(os.path.abspath(__file__))
    except OSError:
        pass
    # V358.1 — «چیزی برای commit نیست» یعنی این نسخه از قبل نشسته است.
    # پیش‌تر این حالت با ❌ مبهم متوقف می‌شد و کاربر فکر می‌کرد نصب شکست
    # خورده؛ در حالی که اغلب فایلِ نصابِ قدیمی‌تری اجرا شده بود (چون
    # الگوی apply_vXX*.py چند نصاب را با هم می‌گیرد و الفبایی اولی را
    # اجرا می‌کند). حالا پیام روشن است و نام نسخه را می‌گوید.
    staged = subprocess.run(["git", "diff", "--cached", "--quiet"]).returncode
    if staged == 0:
        print("\n" + "=" * 62)
        print("این نسخه (" + VERSION + ") از قبل روی این شاخه نصب شده است.")
        print("هیچ تغییری لازم نبود و چیزی خراب نشد — درخت کاری تمیز است.")
        print("")
        print("اگر آهنگ نصب نسخهٔ تازه‌تری داشتید، احتمالاً فایلِ نصابِ")
        print("قدیمی اجرا شده است. پوشهٔ دانلود را ببینید:")
        print("  ls -1 /mnt/c/Users/Hashem/Downloads | grep apply_")
        print("و نصابِ همان نسخه را با الگوی دقیق اجرا کنید؛ مثلاً برای")
        print("نسخهٔ V358.1 الگوی apply_v358_1* تنها همان یک فایل را می‌گیرد.")
        print("=" * 62)
        sys.exit(0)
    run(["git"] + git_ident() + ["commit", "-m", COMMIT_MSG])
    # V298 — مقصد طبق انتخاب کاربر در گفتگو (قاعده: هر بار پرسیده می‌شود: آزمایشی / اصلی / هر دو)
    run(["git", "config", "http.postBuffer", "524288000"], check=False)
    run(["git", "config", "http.lowSpeedLimit", "0"], check=False)
    run(["git", "config", "http.lowSpeedTime", "999999"], check=False)
    if TARGET == "main":
        push_checked("HEAD:refs/heads/main")
        time.sleep(2)
        # V341 — اگر staging کارِ تأییدنشده دارد (جلوتر از main است)، fast-forward رد می‌شود.
        # در آن حالت main را داخل staging ادغام می‌کنیم تا هر دو شاخه اصلاح را داشته باشند
        # و مسیر promote برای نسخهٔ آزمایشیِ در انتظار بسته نشود.
        main_sha = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
        anc = subprocess.run(["git", "merge-base", "--is-ancestor", "origin/staging", main_sha], capture_output=True)
        if anc.returncode != 0:
            print("\nشاخهٔ آزمایشی جلوتر از اصلی است؛ اصلاح روی آن هم ادغام می‌شود…")
            run(["git", "fetch", "origin"])
            run(["git", "checkout", "-B", "staging", "origin/staging"])
            run(["git", "reset", "--hard", "origin/staging"])
            mg = subprocess.run(["git"] + git_ident() + ["merge", "--no-edit", "-m",
                                "Merge main into staging: " + VERSION, main_sha],
                                capture_output=True, text=True)
            if mg.returncode != 0:
                print(mg.stdout + mg.stderr)
                run(["git", "merge", "--abort"], check=False)
                print("⚠ ادغام خودکار ممکن نشد. اصلاح روی main نشست؛ برای staging دستی ادغام کنید:")
                print("   git checkout staging && git merge main")
            else:
                push_checked("HEAD:refs/heads/staging")
                print("✅ اصلاح روی هر دو شاخه (main و staging) نشست.")
        else:
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
