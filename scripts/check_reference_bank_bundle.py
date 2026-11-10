#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V360 — نگهبان «هیچ دادهٔ بانک مرجع داخل اپ یا سایت نیست».

قید کاربر: حجم سؤالها و تصویرها به APK یا باندل سایت اضافه نشود.
این نگهبان جلوی توکار شدن دوبارهٔ همان ۱۶ مگابایت HTML یا ۲۴۶۵ تصویر
را میگیرد. نمونهٔ کاربر فقط در samples/ بیرون مخزن مجاز است.

اجرا:  python3 scripts/check_reference_bank_bundle.py
خودآزمون: python3 scripts/check_reference_bank_bundle.py --selftest
"""
import pathlib
import re
import sys
import tempfile

FORBIDDEN_NAMES = {
    "catalog.json", "catalog.csv", "questions.jsonl",
    "review_report.csv", "corrections.csv", "images_manifest.csv",
    "manifest.json",
}
FORBIDDEN_PREFIXES = ("reference-bank", "refbank")
WATCH = ("app/src/main/assets", "site")
MAX_QUESTION_JSON = 200_000  # primary_bank.json کنونی ۳۶ کیلوبایت است

CJK = re.compile(r"[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\u0900-\u097f]")


def repo_root():
    here = pathlib.Path(__file__).resolve().parent
    for cand in [here] + list(here.parents):
        if (cand / "settings.gradle.kts").is_file():
            return cand
    return here.parent


def scan(root):
    problems = []
    for rel in WATCH:
        base = root / rel
        if not base.is_dir():
            continue
        for p in base.rglob("*"):
            if not p.is_file() or ".git" in p.parts:
                continue
            name = p.name.lower()
            low = str(p.relative_to(root)).lower()
            if name in FORBIDDEN_NAMES:
                problems.append("فایل فهرست/محتوای بانک داخل باندل: " + str(p.relative_to(root)))
            if any(name.startswith(x) for x in FORBIDDEN_PREFIXES) and name.endswith((".json", ".jsonl", ".csv")):
                problems.append("دادهٔ بانک مرجع داخل باندل: " + str(p.relative_to(root)))
            if "reference-bank" in low and name.endswith((".webp", ".png", ".jpg", ".jpeg")):
                problems.append("تصویر بانک مرجع داخل باندل: " + str(p.relative_to(root)))
            # JSON سؤال بزرگتر از ۲۰۰ کیلوبایت، مشکوک به بانک توکار
            if name.endswith(".json") and p.stat().st_size > MAX_QUESTION_JSON and "bank" in low:
                problems.append("JSON بانک بیش از حد مجاز: " + str(p.relative_to(root)))
    # فایل هندآف و نمونهها نباید در مخزن باشند
    for rel in ("app/src/main/assets/bank-soal-paye-aval-matni.html",
                "site/bank-soal-paye-aval-matni.html"):
        if (root / rel).is_file():
            problems.append("HTML نمونه داخل باندل: " + rel)
    return problems


def selftest():
    ok = True
    with tempfile.TemporaryDirectory(prefix="v360_guard_") as d:
        root = pathlib.Path(d)
        (root / "settings.gradle.kts").write_text("", encoding="utf-8")
        (root / "app/src/main/assets").mkdir(parents=True)
        (root / "site").mkdir(parents=True)
        # وضعیت سالم: همان بانک کوچک ابتدایی مجاز است
        (root / "app/src/main/assets/primary_bank.json").write_text("{}", encoding="utf-8")
        a = scan(root)
        print("  حالت سالم:", "PASS" if not a else "FAIL " + str(a))
        ok &= not a
        # فایل ممنوع
        (root / "site/catalog.json").write_text("[]", encoding="utf-8")
        b = scan(root)
        hit = any("catalog.json" in x for x in b)
        print("  فایل فهرست:", "PASS" if hit else "FAIL")
        ok &= hit
        # تصویر ممنوع
        (root / "site/reference-bank/images").mkdir(parents=True)
        (root / "site/reference-bank/images/a.webp").write_bytes(b"x")
        c = scan(root)
        hit2 = any("a.webp" in x for x in c)
        print("  تصویر بانک:", "PASS" if hit2 else "FAIL")
        ok &= hit2
    print("REFERENCE_BANK_BUNDLE_SELFTEST: " + ("PASS" if ok else "FAIL"))
    return 0 if ok else 1


def main():
    if "--selftest" in sys.argv:
        return selftest()
    root = repo_root()
    problems = scan(root)
    # اسکن نویسهٔ بیگانه روی فایلهای تازهٔ مرتبط
    for rel in ("scripts/review_reference_bank.py", "scripts/upload_reference_bank.py",
                "site/src/refbank.js", "scripts/check_reference_bank_bundle.py"):
        p = root / rel
        if p.is_file() and CJK.search(p.read_text(encoding="utf-8", errors="ignore")):
            problems.append("نویسهٔ بیگانه در " + rel)
    if problems:
        print("REFERENCE_BANK_BUNDLE: FAIL")
        for x in problems:
            print("  x", x)
        print("  نمونهها فقط در /home/user/samples/ مجازاند، نه در اپ یا سایت.")
        return 1
    print("REFERENCE_BANK_BUNDLE: PASS — هیچ دادهٔ بانک مرجعی داخل اپ یا سایت نیست")
    return 0


if __name__ == "__main__":
    sys.exit(main())
