#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V363.1 — رفع دو خطای کامپایل در تست V363.

مشکل: در اجرای CI، فایل تست دو خطا داد (کد اصلی اپ سالم کامپایل شد):

    V363_ReferenceBankTextTest.kt:48  No value passed for parameter 'id'
    V363_ReferenceBankTextTest.kt:77  Unresolved reference 'x' / 'y'

علت اول: ‏ReferenceBankQuestion پارامتر «id» را بدون مقدار پیش‌فرض دارد.
علت دوم: در کاتلین «$x» یک template است، نه علامت دلار فرمول؛ باید \\$ نوشته شود.

این اسکریپت فقط همان یک فایل تست را بازنویسی می‌کند (بدون git apply، بدون
تداخل با پچ قبلی)، سپس سلامت آن را بررسی و در صورت خواستن کامیت و push می‌کند.

    python3 V363_1_testfix.py                    # فقط بررسی/نوشتن
    python3 V363_1_testfix.py --commit --push    # نوشتن + کامیت + push
"""

import argparse
import base64
import hashlib
import pathlib
import subprocess
import sys

TARGET = "app/src/test/java/ir/exam/app/V363_ReferenceBankTextTest.kt"
FILE_SHA256 = "83e465c8778416d82250b5e474f5d7006ded0075f15666b117017f958534fce8"
FILE_BYTES = 9258
MARKERS = [
    "id = 1,",
    "\\$\\\\displaystyle x",
    "unbalancedFormulaIsNeverLeftOpen",
    "blankOptionsBecomeEssayUnlessTheyHaveImages",
    "V363",
]


def run(args, cwd):
    p = subprocess.run(args, cwd=str(cwd), capture_output=True, text=True)
    return p.returncode, ((p.stdout or "") + (p.stderr or "")).strip()


def strip_kotlin_literals(text):
    """رشته‌ها و کامنت‌ها را حذف می‌کند تا شمارش براکت‌ها دقیق باشد."""
    out, i, n = [], 0, len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n and text[i] != '"':
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def kotlin_template_risks(text):
    # هر دلاری که بعدش حرف باشد و escape نشده باشد، خطای کامپایل می‌دهد.
    problems, i = [], 0
    while True:
        i = text.find("$", i)
        if i < 0:
            break
        prev = text[i - 1] if i else ""
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if prev != "\\" and (nxt.isalpha() or nxt == "_"):
            problems.append((text.count("\n", 0, i) + 1, nxt))
        i += 1
    return problems


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default="", help="پوشهٔ مخزن (پیش‌فرض: پوشهٔ همین فایل)")
    ap.add_argument("--commit", action="store_true")
    ap.add_argument("--push", action="store_true")
    args = ap.parse_args()

    print("=" * 70)
    print("V363.1 — رفع دو خطای کامپایل تست بانک مرجع")
    print("=" * 70)

    data = base64.b64decode("".join(FILE_B64.split()))
    if hashlib.sha256(data).hexdigest() != FILE_SHA256 or len(data) != FILE_BYTES:
        print("❌ محتوای داخلی فایل خراب است (دانلود ناقص) — دوباره دانلود کنید.")
        return 2
    print("✅ فایل اصلاح‌شده سالم است (%d بایت)" % len(data))

    here = pathlib.Path(__file__).resolve().parent
    repo = pathlib.Path(args.repo).expanduser().resolve() if args.repo else here
    if not (repo / ".git").exists():
        candidates = [p for p in here.iterdir() if p.is_dir() and (p / ".git").exists()]
        if len(candidates) == 1:
            repo = candidates[0]
        else:
            print("\n❌ مخزن پیدا نشد. با --repo مسیرش را بدهید، مثلاً:")
            print("   python3 %s --repo /mnt/c/Users/Hashem/Downloads/exam-app-kotlin" % pathlib.Path(__file__).name)
            return 2

    _, branch = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], repo)
    _, head = run(["git", "log", "--oneline", "-1"], repo)
    _, version = run(["cat", "text/APP_VERSION.txt"], repo)
    target = repo / TARGET
    print("\nمخزن      : %s" % repo)
    print("شاخه      : %s" % branch)
    print("آخرین کامیت: %s" % head)
    print("نسخه      : %s" % version)

    if not target.is_file():
        print("\n❌ فایل تست پیدا نشد: %s" % TARGET)
        print("   یعنی پچ V363 روی این مخزن اعمال نشده است.")
        return 2

    old = target.read_text(encoding="utf-8")
    if hashlib.sha256(old.encode("utf-8")).hexdigest() == FILE_SHA256:
        print("\n🎉 این فایل از قبل اصلاح شده است؛ کاری لازم نیست.")
    else:
        if "No value passed" in old:
            pass
        target.write_text(data.decode("utf-8"), encoding="utf-8")
        print("\n✅ فایل تست بازنویسی شد.")

    text = target.read_text(encoding="utf-8")
    ok = True
    risks = kotlin_template_risks(text)
    print("\nبررسی سلامت فایل:")
    print("   %s template ناخواسته: %s" % ("✅" if not risks else "❌", risks if risks else "هیچ"))
    ok = ok and not risks
    missing = [m for m in MARKERS if m not in text]
    print("   %s نشانه‌های تست: %s" % ("✅" if not missing else "❌", "همه" if not missing else missing))
    ok = ok and not missing
    code = strip_kotlin_literals(text)
    for name, a, b in (("آکولاد", "{", "}"), ("پرانتز", "(", ")")):
        same = code.count(a) == code.count(b)
        print("   %s توازن %s: %d/%d" % ("✅" if same else "❌", name, code.count(a), code.count(b)))
        ok = ok and same
    if not ok:
        print("\n❌ بررسی ناموفق؛ کامیت نمی‌کنم.")
        return 1
    print("\n✅ فایل تست سالم است.")

    if not args.commit:
        print("\nبرای ثبت:")
        print("   git add -A && git commit -m \"V363.1 - fix two compile errors in the V363 reference bank test\"")
        print("   git push origin HEAD")
        print("\nیا همین دستور با --commit --push")
        return 0

    run(["git", "add", TARGET], repo)
    code, out = run(["git", "-c", "user.name=Hashem Amiri", "-c", "user.email=hashem@local",
                     "commit", "-m", "V363.1 - fix two compile errors in the V363 reference bank test"], repo)
    print("\nکامیت: " + ("موفق ✅" if code == 0 else "ناموفق ❌"))
    if out:
        print("   " + out.splitlines()[0])
    _, head2 = run(["git", "log", "--oneline", "-1"], repo)
    print("   HEAD: %s" % head2)

    if args.push:
        code, out = run(["git", "push", "origin", "HEAD"], repo)
        print("\npush: " + ("موفق ✅" if code == 0 else "ناموفق ❌"))
        for line in out.splitlines()[-4:]:
            print("   " + line)

    print("\n" + "=" * 70)
    print("گام بعدی: تب Actions → اجرای جدیدترین کامیت")
    print("انتظار: APP_VERSION_NAME 1.03.93 و «tests completed» بدون failed")
    print("=" * 70)
    return 0


FILE_B64 = (
    "cGFja2FnZSBpci5leGFtLmFwcAoKaW1wb3J0IGlyLmV4YW0uYXBwLmRvbWFpbi5iYW5rLlJlZmVyZW5jZUJhbmtNZWRpYQppbXBv"
    "cnQgaXIuZXhhbS5hcHAuZG9tYWluLmJhbmsuUmVmZXJlbmNlQmFua1F1ZXN0aW9uCmltcG9ydCBpci5leGFtLmFwcC51aS5iYW5r"
    "LnRvQmFua1F1ZXN0aW9uT3B0aW9uCmltcG9ydCBpci5leGFtLmFwcC51aS5idWlsZGVyLlF1ZXN0aW9uVHlwZQppbXBvcnQgamF2"
    "YS5pby5GaWxlCmltcG9ydCBvcmcuanVuaXQuQXNzZXJ0LmFzc2VydEVxdWFscwppbXBvcnQgb3JnLmp1bml0LkFzc2VydC5hc3Nl"
    "cnRUcnVlCmltcG9ydCBvcmcuanVuaXQuVGVzdAoKLyoqCiAqIFYzNjMg4oCUINmF2KrZhiDYs9ik2KfZhCDYqNin2YbaqSDZhdix"
    "2KzYuSDYr9ixINmH2YXZh9mUINm+2KfbjNmH4oCM2YfYpy4KICoKICog2LTYp9ix2K/Zh9in24wg2b7Yp9uM2YfigIzZh9in24wg"
    "27bYjCDbtyDZiCDbuCDZgduM2YTYryBgdGV4dGAg2LHYpyDYrtin2YTbjCDar9iw2KfYtNiq2YfigIzYp9mG2K8g2Ygg2YXYqtmG"
    "INqp2KfZhdmEINmB2YLYtwogKiDYr9ixIGBxX2h0bWxgINin2LPYqi4g2YHZh9ix2LPYqiDYqNin2YbaqSDZiCDYs9ik2KfZhNuM"
    "INqp2Ycg2KjZhyDYotiy2YXZiNmGINin2YHYstmI2K/ZhyDZhduM4oCM2LTZiNivINmH2YXYp9mGIGB0ZXh0YAogKiDYsdinINmF"
    "24zigIzYrtmI2KfZhtiv2YbYr9ibINmG2KrbjNis2Yc6INiz2LfYsSDZiCDYotiy2YXZiNmGINio24zigIzZhdiq2YbYjCDZiNmE"
    "24wg2b7bjNi04oCM2YbZhdin24zYtCDYs9in2YTZhS4KICoKICog2Ybaqdiq2YfZlCDaqdin2KrZhNuM2YY6INmH2LEgwqskwrsg"
    "2qnZhyDYqNi52K/YtCDYrdix2YEg2KjYp9i02K8gdGVtcGxhdGUg2K3Ys9in2Kgg2YXbjOKAjNi02YjYryDZiCDYqNin24zYryDY"
    "qNinCiAqINio2qnigIzYp9iz2YTYtCBlc2NhcGUg2LTZiNivIOKAlCDZh9mF24zZhiDbjNqpINmF2YjYsdivINiv2LEg2KfYrNix"
    "2KfbjCDYp9mI2YQgQ0kg2K/ZiCDYrti32KfbjCDaqdin2YXZvtin24zZhCDYr9in2K8uCiAqLwpjbGFzcyBWMzYzX1JlZmVyZW5j"
    "ZUJhbmtUZXh0VGVzdCB7CgogICAgcHJpdmF0ZSBmdW4gcm9vdCgpOiBGaWxlIHsKICAgICAgICB2YXIgZCA9IEZpbGUoU3lzdGVt"
    "LmdldFByb3BlcnR5KCJ1c2VyLmRpciIpID86ICIuIikKICAgICAgICB3aGlsZSAoIUZpbGUoZCwgInNldHRpbmdzLmdyYWRsZS5r"
    "dHMiKS5leGlzdHMoKSAmJiBkLnBhcmVudEZpbGUgIT0gbnVsbCkgZCA9IGQucGFyZW50RmlsZSEhCiAgICAgICAgcmV0dXJuIGQK"
    "ICAgIH0KCiAgICBwcml2YXRlIGZ1biBzcmMocGF0aDogU3RyaW5nKTogU3RyaW5nID0gRmlsZShyb290KCksIHBhdGgpLnJlYWRU"
    "ZXh0KENoYXJzZXRzLlVURl84KQoKICAgIHByaXZhdGUgdmFsIGJyaWRnZVBhdGggPSAiYXBwL3NyYy9tYWluL2phdmEvaXIvZXhh"
    "bS9hcHAvZG9tYWluL2JhbmsvUmVmZXJlbmNlQmFua01vZGVscy5rdCIKICAgIHByaXZhdGUgdmFsIGRpYWxvZ1BhdGggPSAiYXBw"
    "L3NyYy9tYWluL2phdmEvaXIvZXhhbS9hcHAvdWkvYmFuay9SZWZlcmVuY2VCYW5rRGlhbG9nLmt0IgogICAgcHJpdmF0ZSB2YWwg"
    "c2l0ZVBhdGggPSAic2l0ZS9zcmMvcmVmYmFuay5qcyIKCiAgICAvLyDbsSkg2b7ZhCDYp9i12YTbjCAtLS0tLS0tLS0tLS0tLS0t"
    "LS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0KCiAgICBAVGVzdAogICAgZnVuIGVtcHR5VGV4dEZh"
    "bGxzQmFja1RvUXVlc3Rpb25IdG1sKCkgewogICAgICAgIC8vINmG2YXZiNmG2YfZlCDZiNin2YLYuduMINm+2KfbjNmH2ZQg2YfY"
    "tNiq2YU6IHRleHQg2K7Yp9mE24zYjCBxX2h0bWwg2b7YsQogICAgICAgIHZhbCBodG1sID0gIjxkaXY+PHNwYW4+2KjZhyDZvtix"
    "2LPYtCDYstuM2LEg2K/Ysdio2KfYsdmH2ZQg2KjYrti04oCM2b7YsNuM2LHbjDwvc3Bhbj4gPHNwYW4+2K/ZgtiqINqp2YbbjNiv"
    "PC9zcGFuPjwvZGl2PiIKICAgICAgICBhc3NlcnRFcXVhbHMoCiAgICAgICAgICAgICLZhdiq2YYg2KjYp9uM2K8g2KfYsiBxX2h0"
    "bWwg2LPYp9iu2KrZhyDYtNmI2K8iLAogICAgICAgICAgICAi2KjZhyDZvtix2LPYtCDYstuM2LEg2K/Ysdio2KfYsdmH2ZQg2KjY"
    "rti04oCM2b7YsNuM2LHbjCDYr9mC2Kog2qnZhtuM2K8iLAogICAgICAgICAgICBSZWZlcmVuY2VCYW5rTWVkaWEuZGlzcGxheVRl"
    "eHQoIiIsIGh0bWwpCiAgICAgICAgKQogICAgfQoKICAgIEBUZXN0CiAgICBmdW4gcmVhZHlUZXh0V2luc092ZXJIdG1sKCkgewog"
    "ICAgICAgIC8vINm+2KfbjNmH4oCM2YfYp9uMINuxINiq2Kcg27Ug2YXYqtmGINii2YXYp9iv2Ycg2K/Yp9ix2YbYr9ibINmG2KjY"
    "p9uM2K8g2KjYp9iy2YbZiNuM2LPbjCDYtNmI2K8uCiAgICAgICAgYXNzZXJ0RXF1YWxzKAogICAgICAgICAgICAi2YXYqtmGINii"
    "2YXYp9iv2Ycg2KjYp9uM2K8g2YXZgtiv2YUg2KjYp9i02K8iLAogICAgICAgICAgICAi2YXYqtmGINii2YXYp9iv2YfZlCDYs9in"
    "2LLZhtiv2YciLAogICAgICAgICAgICBSZWZlcmVuY2VCYW5rTWVkaWEuZGlzcGxheVRleHQoItmF2KrZhiDYotmF2KfYr9mH2ZQg"
    "2LPYp9iy2YbYr9mHIiwgIjxkaXY+2obbjNiyINiv24zar9ixPC9kaXY+IikKICAgICAgICApCiAgICAgICAgdmFsIHF1ZXN0aW9u"
    "ID0gUmVmZXJlbmNlQmFua1F1ZXN0aW9uKAogICAgICAgICAgICBpZCA9IDEsCiAgICAgICAgICAgIGdyYWRlID0gNSwKICAgICAg"
    "ICAgICAgdGV4dCA9ICLZhdiq2YYg2KLZhdin2K/Zh9mUINiz2KfYstmG2K/ZhyIsCiAgICAgICAgICAgIHFIdG1sID0gIjxkaXY+"
    "2obbjNiyINiv24zar9ixPC9kaXY+IgogICAgICAgICkKICAgICAgICBhc3NlcnRFcXVhbHMoInByb3BlcnR5INmG2YXYp9uM2LTb"
    "jCDZh9mFINio2KfbjNivINmH2YXYp9mGINmF2KrZhiDYotmF2KfYr9mHINix2Kcg2KjYr9mH2K8iLCAi2YXYqtmGINii2YXYp9iv"
    "2YfZlCDYs9in2LLZhtiv2YciLCBxdWVzdGlvbi5kaXNwbGF5VGV4dCkKICAgIH0KCiAgICBAVGVzdAogICAgZnVuIHplcm9XaWR0"
    "aE5vbkpvaW5lclN1cnZpdmVzKCkgewogICAgICAgIC8vIHBsYWluVGV4dCDZgtiv24zZhduMINmG24zZheKAjNmB2KfYtdmE2Ycg"
    "2LHYpyDZhduM4oCM2LTaqdiz2KogKMKr2YXbjCDYtNmI2K/CuynYmyDZhdiq2YYg2LPYpNin2YQg2YbYqNin24zYryDYqNi02qnZ"
    "htivLgogICAgICAgIGFzc2VydEVxdWFscygKICAgICAgICAgICAgItmG24zZheKAjNmB2KfYtdmE2YfZlCBIVE1MINio2KfbjNiv"
    "INit2YHYuCDYtNmI2K8iLAogICAgICAgICAgICAi2YXbjFx1MjAwY9i02YjYryDbsiArINuzINio2LHYp9io2LEg27UiLAogICAg"
    "ICAgICAgICBSZWZlcmVuY2VCYW5rTWVkaWEuZGlzcGxheVRleHQoIiIsICI8ZGl2PtmF24wmenduajvYtNmI2K8g27IgKyDbsyDY"
    "qNix2KfYqNixINu1PC9kaXY+IikKICAgICAgICApCiAgICAgICAgYXNzZXJ0RXF1YWxzKAogICAgICAgICAgICAi2YbbjNmF4oCM"
    "2YHYp9i12YTZh9mUINiu2YjYr9mQINmF2KrZhiDZh9mFINiv2LPYquKAjNmG2K7ZiNix2K/ZhyDZhduM4oCM2YXYp9mG2K8iLAog"
    "ICAgICAgICAgICAi2YXbjFx1MjAwY9i02YjYryIsCiAgICAgICAgICAgIFJlZmVyZW5jZUJhbmtNZWRpYS5kaXNwbGF5VGV4dCgi"
    "2YXbjFx1MjAwY9i02YjYryIsICIiKQogICAgICAgICkKICAgIH0KCiAgICBAVGVzdAogICAgZnVuIHNuaXBwZXRLZWVwc1dvcmRz"
    "QW5kRm9ybXVsYUJvdW5kYXJpZXMoKSB7CiAgICAgICAgdmFsIGxvbmcgPSAi2KfZhNmBICIucmVwZWF0KDEyMCkgKyAiXCRcXGRp"
    "c3BsYXlzdHlsZSB4XCQiCiAgICAgICAgdmFsIGN1dCA9IFJlZmVyZW5jZUJhbmtNZWRpYS5kaXNwbGF5VGV4dChsb25nLCAiIiwg"
    "NjApCiAgICAgICAgYXNzZXJ0VHJ1ZSgi2KjYsdi0INio2KfbjNivINqp2YjYqtin2Ycg2KjYp9i02K86ICR7Y3V0Lmxlbmd0aH0i"
    "LCBjdXQubGVuZ3RoIDw9IDYxKQogICAgICAgIGFzc2VydFRydWUoItio2LHYtCDYqNin24zYryDYqNinIMKr4oCmwrsg2KrZhdin"
    "2YUg2LTZiNivIiwgY3V0LmVuZHNXaXRoKCLigKYiKSkKICAgICAgICBhc3NlcnRFcXVhbHMoItiq2LnYr9in2K8g2K/ZhNin2LHZ"
    "h9in24wg2YHYsdmF2YjZhCDYqNin24zYryDYrNmB2Kog2KjZhdin2YbYryIsIDAsIGN1dC5jb3VudCB7IGl0ID09ICckJyB9ICUg"
    "MikKICAgICAgICBhc3NlcnRUcnVlKCLaqdmE2YXZhyDZhtio2KfbjNivINmG2LXZgdmHINio2YXYp9mG2K8iLCAhY3V0LnJlbW92"
    "ZVN1ZmZpeCgi4oCmIikuZW5kc1dpdGgoItin2YQiKSkKICAgIH0KCiAgICBAVGVzdAogICAgZnVuIHVuYmFsYW5jZWRGb3JtdWxh"
    "SXNOZXZlckxlZnRPcGVuKCkgewogICAgICAgIC8vINux27cg2LPYpNin2YQg2b7Yp9uM2YfZlCDZh9i02KrZhSDYr9ixINiu2YjY"
    "r9mQINmF2YbYqNi5INmI2LPYtyDZgdix2YXZiNmEINio2LHbjNiv2Ycg2LTYr9mH4oCM2KfZhtivLgogICAgICAgIHZhbCBicm9r"
    "ZW4gPSAi2Kfar9ixIFwkXFxkaXNwbGF5c3R5bGUgeD1cXHRleHR727J9XCQg2YggXCRcXGRpc3BsYXlzdHlsZSB5IgogICAgICAg"
    "IHZhbCBzaG93biA9IFJlZmVyZW5jZUJhbmtNZWRpYS5iYWxhbmNlTWF0aChicm9rZW4pCiAgICAgICAgYXNzZXJ0RXF1YWxzKCLY"
    "r9mE2KfYsdmH2KfbjCDZgdix2YXZiNmEINio2KfbjNivINis2YHYqiDYtNmI2YbYryIsIDAsIHNob3duLmNvdW50IHsgaXQgPT0g"
    "JyQnIH0gJSAyKQogICAgICAgIGFzc2VydFRydWUoItmB2LHZhdmI2YQg2YbbjNmF2YfigIzYqNix24zYr9mHINio2KfbjNivINio"
    "2KcgwqvigKbCuyDYqNiz2KrZhyDYtNmI2K8iLCBzaG93bi5lbmRzV2l0aCgi4oCmIikpCgogICAgICAgIHZhbCBjdXRMb25nID0g"
    "UmVmZXJlbmNlQmFua01lZGlhLmRpc3BsYXlUZXh0KCLYqCAiLnJlcGVhdCg1MCkgKyAiXCR4XCQg2YggXCR5IiwgIiIsIDQwKQog"
    "ICAgICAgIGFzc2VydEVxdWFscygi2KjYsdi0INmH2YUg2YbYqNin24zYryDZgdix2YXZiNmEINix2Kcg2KjYp9iyINio2q/YsNin"
    "2LHYryIsIDAsIGN1dExvbmcuY291bnQgeyBpdCA9PSAnJCcgfSAlIDIpCiAgICB9CgogICAgLy8g27IpINmF2b7YsSDYotiy2YXZ"
    "iNmGIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLQoKICAgIEBUZXN0CiAg"
    "ICBmdW4gcXVlc3Rpb25BZGRlZFRvRXhhbUtlZXBzSXRzVGV4dCgpIHsKICAgICAgICB2YWwgcXVlc3Rpb24gPSBSZWZlcmVuY2VC"
    "YW5rUXVlc3Rpb24oCiAgICAgICAgICAgIGlkID0gNjAwMSwKICAgICAgICAgICAgZ3JhZGUgPSA2LAogICAgICAgICAgICB0eXBl"
    "ID0gIm11bHRpcGxlIiwKICAgICAgICAgICAgdGV4dCA9ICIiLAogICAgICAgICAgICBxSHRtbCA9ICI8ZGl2Ptit2KfYtdmEINuy"
    "ICsg27Mg2qnYr9in2YUg2KfYs9iq2J88L2Rpdj4iLAogICAgICAgICAgICBvcHRpb25zID0gbGlzdE9mKCLbtCIsICLbtSIsICLb"
    "tiIsICLbtyIpLAogICAgICAgICAgICBjb3JyZWN0SW5kZXggPSAxCiAgICAgICAgKQogICAgICAgIHZhbCBkcmFmdCA9IHF1ZXN0"
    "aW9uLnRvQmFua1F1ZXN0aW9uT3B0aW9uKGluY2x1ZGVBbnN3ZXIgPSBmYWxzZSkucXVlc3Rpb24KICAgICAgICBhc3NlcnRFcXVh"
    "bHMoItmG2YjYuSDYqNin24zYryDYqtiz2KrbjCDYqNmF2KfZhtivIiwgUXVlc3Rpb25UeXBlLk1VTFRJUExFX0NIT0lDRSwgZHJh"
    "ZnQudHlwZSkKICAgICAgICBhc3NlcnRFcXVhbHMoItiz2KTYp9mEINin2YHYstmI2K/Zh+KAjNi02K/ZhyDYqNin24zYryDZhdiq"
    "2YYg2K/Yp9i02KrZhyDYqNin2LTYryIsICLYrdin2LXZhCDbsiArINuzINqp2K/Yp9mFINin2LPYqtifIiwgZHJhZnQudGV4dCkK"
    "ICAgICAgICBhc3NlcnRFcXVhbHMoItmH2LEg2obZh9in2LEg2q/YstuM2YbZhyDYqNin24zYryDYqNmF2KfZhtmG2K8iLCA0LCBk"
    "cmFmdC5vcHRpb25zLnNpemUpCiAgICB9CgogICAgQFRlc3QKICAgIGZ1biBibGFua09wdGlvbnNCZWNvbWVFc3NheVVubGVzc1Ro"
    "ZXlIYXZlSW1hZ2VzKCkgewogICAgICAgIHZhbCBibGFua09wdGlvbnMgPSBSZWZlcmVuY2VCYW5rUXVlc3Rpb24oCiAgICAgICAg"
    "ICAgIGlkID0gNjAwMiwKICAgICAgICAgICAgZ3JhZGUgPSA2LAogICAgICAgICAgICB0eXBlID0gIm11bHRpcGxlIiwKICAgICAg"
    "ICAgICAgdGV4dCA9ICIiLAogICAgICAgICAgICBxSHRtbCA9ICI8ZGl2Ptiz2KTYp9mE24wg2qnZhyDar9iy24zZhtmH4oCM2YfY"
    "p9uM2LQg2K7Yp9mE24wg2KfYs9iq2J88L2Rpdj4iLAogICAgICAgICAgICBvcHRpb25zID0gbGlzdE9mKCIiLCAiIiksCiAgICAg"
    "ICAgICAgIGNvcnJlY3RJbmRleCA9IDAKICAgICAgICApCiAgICAgICAgYXNzZXJ0RXF1YWxzKAogICAgICAgICAgICAi2KrYs9iq"
    "24zZkCDYqNuM4oCM2YXYqtmGINio2KfbjNivINiq2LTYsduM2K3bjCDYtNmI2K8iLAogICAgICAgICAgICBRdWVzdGlvblR5cGUu"
    "RVNTQVksCiAgICAgICAgICAgIGJsYW5rT3B0aW9ucy50b0JhbmtRdWVzdGlvbk9wdGlvbihmYWxzZSkucXVlc3Rpb24udHlwZQog"
    "ICAgICAgICkKCiAgICAgICAgdmFsIHdpdGhJbWFnZXMgPSBSZWZlcmVuY2VCYW5rUXVlc3Rpb24oCiAgICAgICAgICAgIGlkID0g"
    "NjAwMywKICAgICAgICAgICAgZ3JhZGUgPSA2LAogICAgICAgICAgICB0eXBlID0gIm11bHRpcGxlIiwKICAgICAgICAgICAgdGV4"
    "dCA9ICIiLAogICAgICAgICAgICBxSHRtbCA9ICI8ZGl2Ptiz2KTYp9mEINiq2LXZiNuM2LHbjDwvZGl2PiIsCiAgICAgICAgICAg"
    "IG9wdGlvbnMgPSBsaXN0T2YoIiIsICIiKSwKICAgICAgICAgICAgY29ycmVjdEluZGV4ID0gMCwKICAgICAgICAgICAgb3B0aW9u"
    "SW1hZ2VzID0gbGlzdE9mKCJodHRwczovL21lZGlhLmV4YW1wbGUvbzEud2VicCIsICJodHRwczovL21lZGlhLmV4YW1wbGUvbzIu"
    "d2VicCIpCiAgICAgICAgKQogICAgICAgIGFzc2VydEVxdWFscygKICAgICAgICAgICAgItqv2LLbjNmG2YfZlCDYqti12YjbjNix"
    "24wg2KjYp9uM2K8g2KrYs9iq24wg2KjZhdin2YbYryIsCiAgICAgICAgICAgIFF1ZXN0aW9uVHlwZS5NVUxUSVBMRV9DSE9JQ0Us"
    "CiAgICAgICAgICAgIHdpdGhJbWFnZXMudG9CYW5rUXVlc3Rpb25PcHRpb24oZmFsc2UpLnF1ZXN0aW9uLnR5cGUKICAgICAgICAp"
    "CiAgICB9CgogICAgLy8g27MpINm+24zZhuKAjNmH2KcgLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0t"
    "LS0tLS0tLS0tLS0tLS0tLS0tLQoKICAgIEBUZXN0CiAgICBmdW4gYXBwU2l0ZVRvb2xBbmRXb3JrZmxvd3NTdGF5V2lyZWQoKSB7"
    "CiAgICAgICAgdmFsIG1vZGVscyA9IHNyYyhicmlkZ2VQYXRoKQogICAgICAgIHZhbCBkaWFsb2cgPSBzcmMoZGlhbG9nUGF0aCkK"
    "ICAgICAgICB2YWwgc2l0ZSA9IHNyYyhzaXRlUGF0aCkKCiAgICAgICAgYXNzZXJ0VHJ1ZSgi2b7ZhCDZhdiq2YYg2K/YsSDZhdiv"
    "2YQiLCAiZnVuIGRpc3BsYXlUZXh0KHRleHQ6IFN0cmluZywgcUh0bWw6IFN0cmluZywgbGltaXQ6IEludCA9IDApIiBpbiBtb2Rl"
    "bHMpCiAgICAgICAgYXNzZXJ0VHJ1ZSgi2YXYqtmGINio2K/ZiNmGINi02qnYs9iq2YYg2YbbjNmF4oCM2YHYp9i12YTZhyIsICJm"
    "dW4gaHRtbFRvVGV4dChodG1sOiBTdHJpbmcpOiBTdHJpbmciIGluIG1vZGVscykKICAgICAgICBhc3NlcnRUcnVlKCLYqNiz2KrZ"
    "hiDZgdix2YXZiNmEINio2KfYsiIsICJmdW4gYmFsYW5jZU1hdGgodmFsdWU6IFN0cmluZyk6IFN0cmluZyIgaW4gbW9kZWxzKQog"
    "ICAgICAgIGFzc2VydFRydWUoItqp2YTbjNivINi02KfYsdivINiq2KfYstmHIiwgIlNIQVJEX1ZFUlNJT04gPSBcInYzNjMwXCIi"
    "IGluIG1vZGVscykKICAgICAgICBhc3NlcnRUcnVlKCLYs9i32LEg2KjbjOKAjNmF2KrZhiDYsdin2YfZhtmF2Kcg2YXbjOKAjNqv"
    "24zYsdivIiwgItiv2LEg2YHZh9ix2LPYqiDYp9uM2YYg2b7Yp9uM2Ycg2YbbjNiz2KoiIGluIGRpYWxvZykKCiAgICAgICAgYXNz"
    "ZXJ0VHJ1ZSgi2LPYp9uM2Kog2YfZhdin2YYg2b7ZhCDYsdinINiv2KfYsdivIiwgInBsYWluUXVlc3Rpb24iIGluIHNpdGUpCiAg"
    "ICAgICAgYXNzZXJ0VHJ1ZSgi2LPYp9uM2Kog2KjYsdi0INin2YXZhiDYr9in2LHYryIsICJmdW5jdGlvbiBzYWZlU25pcHBldCh2"
    "YWx1ZSwgbGltaXQpIiBpbiBzaXRlKQogICAgICAgIGFzc2VydFRydWUoItqp2YTbjNivINi02KfYsdivINiz2KfbjNiqIiwgIlNI"
    "QVJEX1ZFUlNJT04gPSAndjM2MzAnIiBpbiBzaXRlKQoKICAgICAgICB2YWwgdG9vbCA9IEZpbGUocm9vdCgpLCAic2NyaXB0cy9y"
    "ZXBhaXJfcmVmZXJlbmNlX2JhbmtfdGV4dC5weSIpCiAgICAgICAgYXNzZXJ0VHJ1ZSgi2KfYqNiy2KfYsSDYqtix2YXbjNmFINi0"
    "2KfYsdivINio2KfbjNivINmF2YjYrNmI2K8g2KjYp9i02K8iLCB0b29sLmlzRmlsZSkKICAgICAgICB2YWwgdGV4dCA9IHRvb2wu"
    "cmVhZFRleHQoQ2hhcnNldHMuVVRGXzgpCiAgICAgICAgYXNzZXJ0VHJ1ZSgi2KfYqNiy2KfYsSDYqNin24zYryDYrdin2YTYqiDa"
    "r9iy2KfYsdi0INiv2KfYtNiq2Ycg2KjYp9i02K8iLCAiXCItLWNoZWNrXCIiIGluIHRleHQpCiAgICAgICAgYXNzZXJ0VHJ1ZSgi"
    "2KfYqNiy2KfYsSDYqNin24zYryDYtNin2LHYryDYs9ik2KfZhCDYsdinINin2K7YqtuM2KfYsduMINio2q/YsNin2LHYryIsICJc"
    "Ii0td2l0aC1xdWVzdGlvbnNcIiIgaW4gdGV4dCkKICAgICAgICBhc3NlcnRUcnVlKCLYp9io2LLYp9ixINio2KfbjNivINm+2YjY"
    "tNmH2ZQg2YXYrdmE24wg2LHYpyDYqNm+2LDbjNix2K8iLCAiXCItLWxvY2FsLWRpclwiIiBpbiB0ZXh0KQoKICAgICAgICB2YWwg"
    "Z3VhcmQgPSBGaWxlKHJvb3QoKSwgInNjcmlwdHMvY2hlY2tfcmVmZXJlbmNlX2JhbmtfdGV4dC5weSIpCiAgICAgICAgYXNzZXJ0"
    "VHJ1ZSgi2Ybar9mH2KjYp9mGINmF2KrZhiDYqNin2YbaqSDZhdix2KzYuSIsIGd1YXJkLmlzRmlsZSkKICAgICAgICBhc3NlcnRU"
    "cnVlKAogICAgICAgICAgICAi2Ybar9mH2KjYp9mGINiv2LEg2q/Ysdiv2LTigIzaqdin2LEg2KfZviIsCiAgICAgICAgICAgIEZp"
    "bGUocm9vdCgpLCAiLmdpdGh1Yi93b3JrZmxvd3MvYW5kcm9pZC55bWwiKS5yZWFkVGV4dChDaGFyc2V0cy5VVEZfOCkuY29udGFp"
    "bnMoImNoZWNrX3JlZmVyZW5jZV9iYW5rX3RleHQiKQogICAgICAgICkKICAgICAgICBhc3NlcnRUcnVlKAogICAgICAgICAgICAi"
    "2Ybar9mH2KjYp9mGINiv2LEg2q/Ysdiv2LTigIzaqdin2LEg2LPYp9uM2KoiLAogICAgICAgICAgICBGaWxlKHJvb3QoKSwgIi5n"
    "aXRodWIvd29ya2Zsb3dzL3NpdGUueW1sIikucmVhZFRleHQoQ2hhcnNldHMuVVRGXzgpLmNvbnRhaW5zKCJjaGVja19yZWZlcmVu"
    "Y2VfYmFua190ZXh0IikKICAgICAgICApCiAgICB9Cn0K"
)


if __name__ == "__main__":
    sys.exit(main())


