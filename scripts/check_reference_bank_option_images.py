#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V360.8 — نگهبان «تصویر گزینه فقط کنار همان گزینه».

چرا لازم شد: شاردها تصویر گزینهها را فقط در image_urls داشتند و اپ/سایت
هر تصویری را که در answer_html نبود «تصویر سؤال» حساب میکردند؛ پس
تصویر گزینههای تستی زیر متن سؤال میرفت. اکنون شارد questions/g1.json
فیلد option_images دارد (همتراز با options) و سورس اپ/سایت آن را میخواند.

این نگهبان:
  ۱) پین میکند که سازندهٔ شارد، سورس اپ و سورس سایت فیلد تازه را دارند؛
  ۲) شارد واقعی را (با --shard) از نظر همترازی و سالمبودن نشانیها میسنجد؛
  ۳) --selftest روی نمونهٔ ساختگی، هم حالت سالم و هم حالت خراب را میآزماید؛
  ۴) V360.9 — کش شاردها حتماً شکسته شود و شارد کهنه قابل تشخیص باشد.
"""
import argparse
import json
import pathlib
import re
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:200]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def validate_shard(path):
    """بررسی یک شارد questions/gN.json؛ برمیگرداند (تعداد خطاها، گزارش)."""
    try:
        data = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    except Exception as e:
        return 1, ["شارد خوانده نشد: " + str(e)[:160]]
    if not isinstance(data, dict) or not data:
        return 1, ["شارد خالی یا نادرست است"]
    problems, rows_with = [], 0
    for key, q in data.items():
        if not isinstance(q, dict):
            problems.append(key + ": ردیف شیء نیست")
            continue
        opts = q.get("options") or []
        oi = q.get("option_images")
        if oi is None:
            problems.append(key + ": option_images وجود ندارد")
            continue
        if not isinstance(oi, list) or len(oi) != len(opts):
            problems.append(key + ": طول option_images با options برابر نیست")
            continue
        allu = set(q.get("image_urls") or [])
        for i, u in enumerate(oi):
            if not u:
                continue
            rows_with += 1
            if u not in allu:
                problems.append(key + ": تصویر گزینه " + str(i) + " در image_urls نیست")
            # همرسانی یک تصویر میان گزینه و توضیح پاسخ در دادهٔ واقعی
            # مجاز است (تصویر یکسان، دو جای درست)؛ خطا محسوب نمیشود.
    return len(problems), problems, rows_with


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--shard", help="مسیر شارد questions/gN.json برای بررسی واقعی")
    ap.add_argument("--selftest", action="store_true")
    args = ap.parse_args()

    print("=== پینهای سورس ===")
    review = (ROOT / "scripts/review_reference_bank.py").read_text(encoding="utf-8")
    ok("سازندهٔ شارد option_images مینویسد", '"option_images": option_images' in review)
    ok("ترتیب تصویر گزینهها حفظ میشود", "image_urls_in_order" in review)
    models = (ROOT / "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt").read_text(encoding="utf-8")
    ok("مدل اپ فیلد تازه را میخواند", '@SerialName("option_images")' in models)
    ok("صورت سؤال فقط تصویرهای q_html را میگیرد", "imageUrlsFromHtml(qHtml)" in models)
    mapper = (ROOT / "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt").read_text(encoding="utf-8")
    ok("نگاشت اپ تصویر گزینهها را میگذارد", "optionImages = optionImagesForDraft" in mapper)
    ok("نگاشت اپ تصویر گزینهها را وارد متن سؤال نمیکند", "questionImageUrls.take(8)" in mapper)
    ref = (ROOT / "site/src/refbank.js").read_text(encoding="utf-8")
    ok("سایت option_images میخواند", "p.option_images" in ref)
    ok("سایت optionImages میسازد", "q.optionImages = q.type === 'multiple'" in ref)
    ok("سایت صورت سؤال را از q_html میگیرد", "imgUrls(p.q_html || '')" in ref)
    uploader = (ROOT / "scripts/upload_reference_bank.py").read_text(encoding="utf-8")
    ok("بارگذار تکفایل دارد", '"--only"' in uploader)

    print("\n=== V360.9 — شارد تازه حتماً خوانده شود ===")
    ok("اپ کلید نسخه دارد", 'SHARD_VERSION = "v3609"' in models)
    ok("اپ نشانی نسخهدار میسازد", "fun shardUrl(path: String)" in models)
    ok("مخزن اپ از نشانی نسخهدار میخواند",
       "ReferenceBankContract.shardUrl(path)" in
       (ROOT / "app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt").read_text(encoding="utf-8"))
    ok("اپ شارد کهنه را تشخیص میدهد", "val shardIsStale: Boolean" in models)
    ok("اپ null بودن فیلد را نگه میدارد", "List<String?>? = null" in models)
    ok("سایت کلید نسخه دارد", "SHARD_VERSION = 'v3609'" in ref)
    ok("سایت کش مرورگر را دور میزند", "cache: 'no-store'" in ref)
    ok("سایت حافظهٔ درونبرنامهای را پاک میکند", "cache.catalog = {}" in ref)
    ok("سایت شارد کهنه را نشان میدهد", "p.option_images === undefined" in ref)
    ok("دیگر force-cache باقی نمانده", "cache: 'force-cache'" not in ref)
    ok("دکمهٔ بررسی شارد در رابط حذف شده", "بررسی شارد" not in ref)
    builder = (ROOT / "site/src/builder.js").read_text(encoding="utf-8")
    ok("توضیح پاسخ با ویرایشگر نماددار است", "tokenTextarea(ansTa, {small: true})" in builder)
    shell = (ROOT / "site/src/app.js").read_text(encoding="utf-8")
    ok("بزرگنمایی یکپارچهٔ تصاویر هست", "function openImagePreview(src, alt)" in shell)
    ok("تصویر کوچک با cursor zoom دارد", "arena-image-preview-thumb" in shell)
    ok("ضربدر بستن تصویر دارد", "بستن تصویر" in shell)

    print("\n=== شارد ===")
    if args.shard:
        result = validate_shard(args.shard)
        if len(result) == 3:
            n, problems, rows_with = result
        else:
            n, problems, rows_with = result[0], result[1], 0
        ok("شارد سالم است", n == 0, problems[:5])
        print("  ردیفهای دارای تصویر گزینه:", rows_with)

    if args.selftest:
        print("\n=== خودآزمون ===")
        good = {"1": {
            "options": ["الف", "ب"],
            "option_images": ["https://x/o1.webp", "https://x/o2.webp"],
            "q_html": '<div><img src="https://x/q.webp"></div>',
            "answer_html": '<div><img src="https://x/a.webp"></div>',
            "image_urls": ["https://x/q.webp", "https://x/o1.webp", "https://x/o2.webp", "https://x/a.webp"],
        }}
        bad_len = json.loads(json.dumps(good))
        bad_len["1"]["option_images"] = ["https://x/o1.webp"]
        bad_url = json.loads(json.dumps(good))
        bad_url["1"]["option_images"] = ["https://x/o1.webp", "https://y/nope.webp"]
        with tempfile.TemporaryDirectory(prefix="v360_8_guard_") as d:
            root = pathlib.Path(d)
            for name, payload, expect_ok in (("good", good, True), ("bad_len", bad_len, False), ("bad_url", bad_url, False)):
                f = root / (name + ".json")
                f.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
                n = validate_shard(f)[0]
                ok("نمونهٔ " + name + " درست تشخیص داده شد", (n == 0) == expect_ok, n)

    print()
    if fails:
        print("REFERENCE_BANK_OPTION_IMAGES: FAIL (%d مورد)" % len(fails))
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("REFERENCE_BANK_OPTION_IMAGES: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
