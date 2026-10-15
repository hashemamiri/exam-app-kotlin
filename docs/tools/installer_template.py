#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""بستهٔ نصب خودکار — اجرا در پوشهٔ پروژه:
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && python3 /mnt/c/Users/Hashem/Downloads/apply_vXX.py
"""
import base64, gzip, io, os, subprocess, sys, tarfile

VERSION = "VXX"
COMMIT_MSG = "..."
FILES = []
REMOVE = []
PAYLOAD = """"""


def run(cmd, check=True):
    print("$ " + " ".join(cmd), flush=True)
    r = subprocess.run(cmd)
    if check and r.returncode != 0:
        print("\n❌ خطا در اجرای دستور بالا — کار متوقف شد (چیزی commit نشد).")
        sys.exit(r.returncode)
    return r.returncode


def main():
    if not (os.path.isdir(".git") and os.path.isfile("scripts/verify_native_final.py")):
        print("❌ این دستور را باید داخل پوشهٔ پروژهٔ exam-app-kotlin اجرا کنید:")
        print("   cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin")
        sys.exit(1)
    print("نصب " + VERSION + " ...")
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
    run(["git", "add", "-A"])
    run(["git", "diff", "--cached", "--check"])
    try:
        os.remove(os.path.abspath(__file__))
    except OSError:
        pass
    run(["git", "-c", "user.name=Hashem Amiri", "-c", "user.email=hashemamiri@users.noreply.github.com",
         "commit", "-m", COMMIT_MSG])
    # V298 — ابتدا به شاخهٔ آزمایشی (staging) می‌رود؛ پس از دیدن و تأیید: python3 scripts/promote.py
    run(["git", "push", "origin", "HEAD:refs/heads/staging"])
    print("\n✅ " + VERSION + " نصب و به شاخهٔ آزمایشی (staging) ارسال شد.")
    print("   GitHub Actions نسخهٔ آزمایشی را می‌سازد: اپ «آزمون (آزمایشی)» + سایت آزمایشی (نشانی در Summary اجرای «site» شاخهٔ staging)")
    print("   پس از بررسی و تأیید:  python3 scripts/promote.py      (انصراف:  python3 scripts/promote.py --cancel)")


if __name__ == "__main__":
    main()
