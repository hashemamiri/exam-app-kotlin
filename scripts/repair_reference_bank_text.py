#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V363 — ترمیم «متن» شاردهای بانک مرجع.

چرا این فایل هست:
    شاردهای منتشرشدهٔ پایه‌های ۶، ۷ و ۸ فیلد ``text`` را خالی گذاشته‌اند و
    متن کامل فقط داخل ``q_html`` است. نتیجه در اپ و سایت:

      • فهرست بانک مرجع: هر سطر بی‌متن (چون فهرست فقط ``text`` را دارد)؛
      • سؤالی که به آزمون افزوده می‌شد: بی‌متن (همان ``text``)؛
      • پیش‌نمایش: سالم، چون ``q_html`` را می‌خواند.

    این ابزار همان متن را از ``q_html`` می‌سازد (بدون شکستن نیم‌فاصله و
    بدون باز گذاشتن فرمولِ نیمه‌بریده) و در دو جا می‌نویسد:

      ۱) ``catalog/gN.json``  — تا فهرست بانک در همهٔ نسخه‌های نصب‌شده
         (که همین catalog را می‌خوانند) بی‌به‌روزرسانی اپ متن پیدا کند.
      ۲) ``questions/gN.json`` — برای دستگاه‌هایی که نسخهٔ قدیمی اپ را دارند
         (پایهٔ ۸ شارد سؤالش هم روی سرور نبود؛ آن را باید بفرستید).

دو حالت اجرا:

  الف) از روی شبکه (برای ۶ و ۷ که شارد سؤالشان روی سرور هست):
      python3 scripts/repair_reference_bank_text.py --check
      python3 scripts/repair_reference_bank_text.py --grades 6,7 --out ~/refbank-repair

  ب) از روی پوشهٔ publish خودتان (برای ۸ و هر پایه‌ای که شارد سؤالش روی
     سرور نیست؛ هیچ اتصال شبکه‌ای لازم نیست):
      python3 scripts/repair_reference_bank_text.py --local-dir ~/publish \
          --out ~/refbank-repair --only-catalog

  سپس بارگذاری با همان ابزار موجود:
      python3 scripts/upload_reference_bank.py --dir ~/refbank-repair \
          --env-file ~/.exam_s3 --apply
"""

import argparse
import json
import pathlib
import re
import sys
import urllib.error
import urllib.request

BASE = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank"
DEFAULT_GRADES = "6,7,8"
DEFAULT_LIMIT = 180
UA = "exam-app-reference-bank-repair/1.1"

ENTITY = {
    "&zwnj;": "\u200c",
    "&nbsp;": " ",
    "&amp;": "&",
    "&lt;": "<",
    "&gt;": ">",
    "&quot;": '"',
    "&#39;": "'",
}


# --------------------------------------------------------------------------
# هستهٔ مشترک — آینهٔ ReferenceBankMedia در اپ
# --------------------------------------------------------------------------

def html_to_text(html):
    """HTML → متن ساده؛ نیم‌فاصله می‌ماند (آینهٔ htmlToText کاتلین)."""
    raw = str(html or "")
    if not raw.strip():
        return ""
    text = re.sub(r"<[^>]*>", " ", raw)
    for key, value in ENTITY.items():
        text = text.replace(key, value)
    text = text.replace("\u00a0", " ")
    return re.sub(r"\s+", " ", text).strip()


def balance_math(value):
    """V363 — اگر تعداد «$» فرد بود (فرمولِ نیمه‌بریده در منبع)، آخرین فرمول حذف شود."""
    value = str(value or "").rstrip()
    if value.count("$") % 2 == 0:
        return value
    cut = value[: value.rfind("$")].rstrip()
    return (cut + "…") if cut else value


def safe_snippet(value, limit):
    """برش امن: نه وسط «$»، نه نصفه‌کلمه، نه فرمولِ باز."""
    value = str(value or "").strip()
    if limit <= 0 or len(value) <= limit:
        return balance_math(value)
    cut = value[:limit]
    if cut.count("$") % 2 == 1:
        cut = cut[: cut.rfind("$")]
    if " " in cut:
        head = cut.rsplit(" ", 1)[0]
        if len(head) > limit // 2:
            cut = head
    return balance_math(cut.rstrip() + "…")


# --------------------------------------------------------------------------
# حالت الف: خواندن از شبکه
# --------------------------------------------------------------------------

def fetch_json(url, timeout=180):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def fetch_or_none(url, timeout=180):
    try:
        return fetch_json(url, timeout=timeout)
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    except urllib.error.URLError as exc:
        print(f"    ! شبکه: {exc}")
        return None


def repair_grade(grade, out_dir, limit, with_questions, check_only, timeout):
    print(f"\n=== پایهٔ {grade} ===")
    catalog_url = f"{BASE}/catalog/g{grade}.json"
    questions_url = f"{BASE}/questions/g{grade}.json"

    catalog = fetch_or_none(catalog_url, timeout)
    if catalog is None:
        print("  ✗ catalog/g%d.json روی سرور نیست؛ این پایه اصلاً منتشر نشده." % grade)
        return None

    questions = fetch_or_none(questions_url, timeout)
    if questions is None:
        print("  ✗ questions/g%d.json روی سرور نیست (شارد سؤال ناقص است)." % grade)
        print("     فهرستش هست ولی پیش‌نمایش/افزودن به آزمون برای این پایه کار نمی‌کند؛")
        print("     با --local-dir روی پوشهٔ publish خودتان ترمیم و بارگذاری کنید.")
    else:
        print(f"  شارد سؤال: {len(questions)} سؤال — فهرست: {len(catalog)} سطر")

    blank_before = sum(1 for row in catalog if not str(row.get("text") or "").strip())
    filled = 0
    no_source = 0
    for row in catalog:
        if str(row.get("text") or "").strip():
            continue
        payload = (questions or {}).get(str(row.get("id")))
        source = ""
        if payload:
            source = html_to_text(payload.get("q_html") or payload.get("text") or "")
        if not source:
            no_source += 1
            continue
        row["text"] = safe_snippet(source, limit)
        filled += 1
        if payload and not row.get("image_urls"):
            urls = payload.get("image_urls") or []
            if urls:
                row["image_urls"] = list(urls)
                row["image_count"] = len(urls)
                row["has_image"] = True

    if with_questions and questions:
        q_filled = 0
        for payload in questions.values():
            if str(payload.get("text") or "").strip():
                continue
            source = html_to_text(payload.get("q_html") or "")
            if source:
                payload["text"] = safe_snippet(source, limit)
                q_filled += 1
        print(f"  شارد سؤال: {q_filled} متن خالی پر شد")

    print(f"  فهرست: {blank_before} سطر بی‌متن بود؛ {filled} سطر پر شد", end="")
    if no_source:
        print(f"؛ {no_source} سطر منبعی در شارد سؤال نداشت", end="")
    print()

    if check_only:
        return {"grade": grade, "blank_before": blank_before, "filled": filled,
                "questions_missing": questions is None}

    if not filled and not (with_questions and questions):
        print("  چیزی برای نوشتن نیست (این پایه سالم است).")
        return {"grade": grade, "blank_before": blank_before, "filled": 0,
                "questions_missing": questions is None}

    base = pathlib.Path(out_dir)
    (base / "catalog").mkdir(parents=True, exist_ok=True)
    cat_path = base / "catalog" / f"g{grade}.json"
    cat_path.write_text(json.dumps(catalog, ensure_ascii=False), encoding="utf-8")
    print(f"  نوشته شد: {cat_path}  ({cat_path.stat().st_size / 1048576:.1f} MB)")
    if with_questions and questions:
        (base / "questions").mkdir(parents=True, exist_ok=True)
        q_path = base / "questions" / f"g{grade}.json"
        q_path.write_text(json.dumps(questions, ensure_ascii=False), encoding="utf-8")
        print(f"  نوشته شد: {q_path}  ({q_path.stat().st_size / 1048576:.1f} MB)")
    return {"grade": grade, "blank_before": blank_before, "filled": filled,
            "questions_missing": questions is None}


# --------------------------------------------------------------------------
# حالت ب: خواندن از پوشهٔ publish محلی (برای پایه‌ای که شارد سؤالش روی سرور نیست)
# --------------------------------------------------------------------------

def load_json_file(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"  ! خواندن {path.name} نشد: {exc}")
        return None


def grades_in(folder):
    found = []
    catalog_dir = folder / "catalog"
    if catalog_dir.is_dir():
        for path in sorted(catalog_dir.glob("g*.json")):
            match = re.fullmatch(r"g(\d+)\.json", path.name)
            if match:
                found.append(int(match.group(1)))
    return sorted(set(found))


def repair_local(grade, folder, out, limit, only_catalog, apply_changes):
    print(f"\n=== پایهٔ {grade} (از پوشهٔ محلی) ===")
    catalog_path = folder / "catalog" / f"g{grade}.json"
    questions_path = folder / "questions" / f"g{grade}.json"

    catalog = load_json_file(catalog_path)
    if not isinstance(catalog, list):
        print(f"  ✗ {catalog_path} فهرست معتبر (لیست) نیست؛ رد شد.")
        return None
    questions = load_json_file(questions_path) if questions_path.is_file() else None
    if questions_path.is_file() and not isinstance(questions, dict):
        print("  ⚠ شارد سؤال دیکشنری نیست؛ فقط از فهرست استفاده می‌شود.")
        questions = None
    print(f"  فهرست: {len(catalog)} سطر" + (
        f" — شارد سؤال: {len(questions)} مورد" if questions else " — شارد سؤال موجود نیست"))

    blank_before = sum(1 for row in catalog if not str(row.get("text") or "").strip())
    filled = 0
    no_source = 0
    images_added = 0
    for row in catalog:
        if str(row.get("text") or "").strip():
            continue
        payload = (questions or {}).get(str(row.get("id")))
        source = html_to_text(payload.get("q_html") or payload.get("text") or "") if payload else ""
        if not source:
            no_source += 1
            continue
        row["text"] = safe_snippet(source, limit)
        filled += 1
        if payload and not row.get("image_urls"):
            urls = payload.get("image_urls") or []
            if urls:
                row["image_urls"] = list(urls)
                row["image_count"] = len(urls)
                row["has_image"] = True
                images_added += 1

    q_filled = 0
    if questions and not only_catalog:
        for payload in questions.values():
            if str(payload.get("text") or "").strip():
                continue
            source = html_to_text(payload.get("q_html") or "")
            if source:
                payload["text"] = safe_snippet(source, limit)
                q_filled += 1

    print(f"  فهرست: {blank_before} سطر بی‌متن → {filled} پر شد"
          + (f"؛ {no_source} سطر منبعی نداشت" if no_source else "")
          + (f"؛ {images_added} تصویر فهرست کامل شد" if images_added else ""))
    if questions and not only_catalog:
        print(f"  شارد سؤال: {q_filled} متن خالی پر شد")

    if not apply_changes:
        return {"grade": grade, "blank_before": blank_before, "filled": filled,
                "q_filled": q_filled, "questions_missing": questions is None}

    out.mkdir(parents=True, exist_ok=True)
    if filled:
        target = out / "catalog" / f"g{grade}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(catalog, ensure_ascii=False), encoding="utf-8")
        print(f"  نوشته شد: {target}  ({target.stat().st_size / 1048576:.1f} MB)")
    else:
        print("  فهرست تغییری لازم نداشت.")
    if questions and not only_catalog and q_filled:
        target = out / "questions" / f"g{grade}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(questions, ensure_ascii=False), encoding="utf-8")
        print(f"  نوشته شد: {target}  ({target.stat().st_size / 1048576:.1f} MB)")
    return {"grade": grade, "blank_before": blank_before, "filled": filled,
            "q_filled": q_filled, "questions_missing": questions is None}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--grades", default=DEFAULT_GRADES,
                        help="پایه‌ها با کاما؛ پیش‌فرض 6,7,8")
    parser.add_argument("--out", default="refbank-repair", help="پوشهٔ خروجی برای بارگذاری")
    parser.add_argument("--limit", type=int, default=DEFAULT_LIMIT,
                        help="حداکثر نویسهٔ متن فهرست؛ پیش‌فرض ۱۸۰")
    parser.add_argument("--with-questions", action="store_true",
                        help="متن شارد سؤال هم پر شود (حجیم؛ برای اپ‌های قدیمی)")
    parser.add_argument("--check", action="store_true", help="فقط گزارش؛ هیچ فایلی نوشته نمی‌شود")
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--local-dir", default="",
                        help="پوشهٔ publish محلی (catalog/ و questions/) — بدون شبکه")
    parser.add_argument("--only-catalog", action="store_true",
                        help="در حالت محلی، شارد سؤال کپی نشود (سبک‌تر)")
    args = parser.parse_args()

    try:
        grades = [int(x) for x in args.grades.replace("،", ",").split(",") if x.strip()]
    except ValueError:
        print("قالب --grades درست نیست؛ نمونه: --grades 6,7,8")
        return 2

    local = pathlib.Path(args.local_dir).expanduser() if args.local_dir else None
    if local is not None:
        if not local.is_dir():
            print(f"پوشهٔ {local} پیدا نشد.")
            return 2
        if args.grades == DEFAULT_GRADES:
            found = grades_in(local)
            if found:
                grades = found
        print("بانک مرجع — ترمیم متن (از پوشهٔ محلی)" + (" — فقط گزارش" if args.check else ""))
        print(f"منبع: {local}")
        results = [repair_local(g, local, pathlib.Path(args.out).expanduser(), args.limit,
                                args.only_catalog, apply_changes=not args.check)
                   for g in grades]
    else:
        print("بانک مرجع — ترمیم متن (از شبکه)" + (" — فقط گزارش" if args.check else ""))
        results = [repair_grade(g, args.out, args.limit, args.with_questions,
                                args.check, args.timeout) for g in grades]

    print("\n=== خلاصه ===")
    for item in results:
        if not item:
            continue
        state = "شارد سؤال ندارد" if item.get("questions_missing") else "سالم"
        extra = f"، شارد سؤال {item['q_filled']}" if item.get("q_filled") else ""
        print(f"  پایهٔ {item['grade']}: {item['blank_before']} بی‌متن → "
              f"{item['filled']} پر شد{extra} ({state})")

    missing = [str(i["grade"]) for i in results if i and i.get("questions_missing")]
    if missing:
        print("\n⚠ شارد سؤال این پایه‌ها روی سرور نیست: " + "، ".join(missing))
        print("  با --local-dir روی پوشهٔ publish خودتان ترمیم و بارگذاری کنید.")

    if args.check:
        print("\nحالت گزارش بود؛ چیزی نوشته نشد.")
        return 0

    print("\n=== بارگذاری (WSL) ===")
    print("  python3 scripts/upload_reference_bank.py --dir %s \\" % args.out)
    print("      --env-file ~/.exam_s3 --apply")
    print("\nبرای حجم کمتر، فقط فهرست‌ها:")
    print("  python3 scripts/upload_reference_bank.py --dir %s --only catalog \\" % args.out)
    print("      --env-file ~/.exam_s3 --apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
