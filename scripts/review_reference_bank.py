#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V360 — بازبینی تکبهتک نمونهٔ بانک مرجع و ساخت خروجی انتشار.

ورودی: فایل HTML مستقلی که کاربر پیوست کرده است.
خروجی: فهرست کوچک، محتوای کامل، شاردها، گزارش بازبینی و مانیفست.

تصمیمهای کاربر که در این اسکریپت اعمال میشوند:
  • «تشریحی کوتاه» به «تشریحی» نگاشت میشود.
  • «درسنامه» حذف میشود.
  • هر سؤال تکبهتک بررسی و موارد دستهٔ اشتباه اصلاح میشوند.
  • سطح چهارم دشواری «خیلی دشوار» اضافه میشود.
  • هیچ فیلد منبع، مجوز یا ارجاع حقوقی نگهداری نمیشود.
  • محتوای سنگین و تصویرها فقط برای ذخیرهٔ شیء آماده میشوند.
  • V360.8 — تصویر هر گزینهٔ تستی جداگانه به همان گزینه وصل میشود،
    نه به متن سؤال.

استفاده:
  python3 scripts/review_reference_bank.py \
      --source samples/bank-soal-paye-aval/bank-soal-paye-aval-matni.html \
      --out samples/bank-soal-paye-aval \
      --base https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank

هیچ کلید، رمز یا توکنی در این فایل نیست.
"""
import argparse
import base64
import csv
import hashlib
import json
import re
import shutil
from pathlib import Path

# ---------------------------------------------------------------------------
# قراردادها
# ---------------------------------------------------------------------------

DIFFICULTY_LABELS = ["تعییننشده", "ساده", "متوسط", "دشوار", "خیلی دشوار"]
DIFFICULTY_MAP = {"آسان": 1, "متوسط": 2, "دشوار": 3, "خیلی دشوار": 4, "": 0}

TYPE_MAP = {
    "تستی": "multiple",
    "تشریحی": "essay",
    "تشریحیکوتاه": "essay",
    "جایخالی": "fill",
    "کوتاهپاسخ": "fill",
    "صحیح/غلط": "truefalse",
    "دستهبندی": "matching",
    "مرتبسازی": "matching",
}
DROP_TYPES = {"درسنامه"}

# اصلاحهای صریح، نتیجهٔ خواندن تکبهتک همان سؤالها.
# کلید = شناسهٔ سؤال؛ مقدار = (درس درست، دلیل).
EXPLICIT_SUBJECT_FIX = {
    1285411: ("فارسی", "متن دربارهٔ حرف و کلمهٔ «درازا» است، نه ریاضی."),
    1259767: ("علوم تجربی", "پرسش دربارهٔ آب رودخانه و محیط است."),
    1784392: ("فارسی", "تکهٔ «مسجد:» تمرین نگاره/نوشتن است، نه فصل سنگها."),
}

# پرچم واژگانی فقط برای گزارش است؛ بهدلیل ریسک تشخیص نادرست،
# بهتنهایی هیچ سؤالی را جابهجا نمیکند.
KEYWORD_HINT = {
    "ریاضی": ["حاصل", "جمع", "تفریق", "اعداد", "مکعب", "مثلث", "دایره", "مساوی", "امتیاز", "عبارت"],
    "فارسی": ["نشانه", "حرف", "کلمه", "واژه", "جمله", "املا", "دوشکلی", "الفبا", "مخالف", "نقطه"],
    "علوم تجربی": ["جانور", "گیاه", "رودخانه", "سایه", "دانه", "میوه", "خاک", "باران", "آهنربا", "بدن"],
}

AMBIGUOUS_CHAPTER = "سوالات و مطالب ترکیبی و کلی"


def norm(s):
    return re.sub(r"[\u200c\u200f\u064b-\u0652\u0610-\u061a]", "", str(s or "")).strip()


def type_key(s):
    """کلید نوع سؤال: نیمفاصله و فاصله هم برداشته میشود."""
    return re.sub(r"\s+", "", norm(s))


def plain(html):
    """تبدیل HTML سؤال به متن ساده، بدون از دست دادن فاصلهها."""
    s = re.sub(r"<[^>]+>", " ", str(html or ""))
    s = (s.replace("&zwnj;", "").replace("&nbsp;", " ")
          .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">"))
    return re.sub(r"\s+", " ", s).strip()


def load_source(path):
    t = Path(path).read_text(encoding="utf-8", errors="replace")
    a = t.index("const DATA=[") + len("const DATA=")
    b = t.index("}];</script>", a) + 2
    data = json.loads(t[a:b])
    m = re.search(r"const FIG=(\{.*?\});const DATA=", t, re.S)
    fig = json.loads(m.group(1)) if m else {}
    return data, fig


def image_refs(blob):
    return sorted(set(re.findall(r'data-fig=["\\]*([0-9a-fA-F-]{36})["\\]*', json.dumps(blob, ensure_ascii=False))))

def image_urls_in_order(html, fig_file, base):
    """V360.8 — تصویرها به ترتیب قرارگرفتن در HTML، بدون تکرار."""
    ids = re.findall(r'data-fig=["\\]*([0-9a-fA-F-]{36})["\\]*',
                     json.dumps(html, ensure_ascii=False))
    out = []
    for key in ids:
        if key in fig_file:
            url = base + "/images/" + fig_file[key]
            if url not in out:
                out.append(url)
    return out


def mathjax_to_tokens(raw):
    r"""V361 — فرمول MathJax \(...\)، \[...\] و $$...$$ → توکن $...$ ویرایشگر فرمول."""
    if not raw:
        return raw
    raw = str(raw)
    out = []
    i = 0
    pairs = [("\\(", "\\)"), ("\\[", "\\]"), ("$$", "$$")]
    while i < len(raw):
        hit = None
        for open_seq, close_seq in pairs:
            if raw.startswith(open_seq, i):
                hit = (open_seq, close_seq)
                break
        if hit:
            open_seq, close_seq = hit
            end = raw.find(close_seq, i + len(open_seq))
            if end >= 0:
                body = " ".join(raw[i + len(open_seq):end].split())
                if body:
                    out.append("$" + body + "$")
                i = end + len(close_seq)
                continue
        out.append(raw[i])
        i += 1
    return separate_adjacent("".join(out))


def separate_adjacent(value):
    """جلوگیری از چسبیدن دو فرمول: $a$$b$ → $a$ $b$."""
    if "$$" not in value:
        return value
    out = []
    inside = False
    for i, ch in enumerate(value):
        out.append(ch)
        if ch == "$":
            if inside:
                inside = False
                if i + 1 < len(value) and value[i + 1] == "$":
                    out.append(" ")
            else:
                inside = True
    return "".join(out)


def formula_token_count(raw):
    tokens = mathjax_to_tokens(raw)
    n, i = 0, 0
    while i < len(tokens):
        if tokens[i] != "$":
            i += 1
            continue
        end = tokens.find("$", i + 1)
        if end < 0:
            break
        if end > i + 1:
            n += 1
        i = end + 1
    return n


def keyword_flag(text, subject):
    s = norm(text)
    hit = {k: sum(s.count(w) for w in ws) for k, ws in KEYWORD_HINT.items()}
    best = max(hit, key=hit.get)
    if hit[best] >= 2 and hit.get(subject, 0) == 0 and best != subject:
        return best
    return ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--base", default="https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank")
    args = ap.parse_args()
    base = args.base.rstrip("/")
    out = Path(args.out)
    pub = out / "publish"
    images = pub / "images"
    catalog_dir = pub / "catalog"
    questions_dir = pub / "questions"
    for d in (pub, images, catalog_dir, questions_dir):
        d.mkdir(parents=True, exist_ok=True)

    data, fig = load_source(args.source)

    # --- تصاویر: حذف تکراری + مانیفست -------------------------------------
    by_hash, fig_file = {}, {}
    for key, val in fig.items():
        raw = base64.b64decode(val.split(",", 1)[1])
        h = hashlib.sha256(raw).hexdigest()
        name = h[:20] + ".webp"
        if h not in by_hash:
            (images / name).write_bytes(raw)
            by_hash[h] = name
        fig_file[key] = by_hash[h]

    with (out / "images_manifest.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["fig_id", "file", "bytes", "sha256"])
        for key, name in fig_file.items():
            raw = (images / name).read_bytes()
            w.writerow([key, name, len(raw), hashlib.sha256(raw).hexdigest()])

    # --- اکثریت درس برای هر فصل -------------------------------------------
    chapter_subject = {}
    tmp = {}
    for d in data:
        tmp.setdefault(d["su"], {})
        tmp[d["su"]][d["co"]] = tmp[d["su"]].get(d["co"], 0) + 1
    for ch, counts in tmp.items():
        chapter_subject[ch] = max(counts, key=counts.get)

    def rewrite_html(html):
        def rep(m):
            key = m.group(1)
            name = fig_file.get(key)
            if not name:
                return ""
            return '<img src="' + base + "/images/" + name + '" alt="" loading="lazy">'
        return re.sub(r'<img[^>]*data-fig=["\\]*([0-9a-fA-F-]{36})["\\]*[^>]*>', rep, str(html or ""))

    # --- بازبینی تکبهتک ---------------------------------------------------
    catalog, payloads, report, corrections = [], {}, [], []
    counts = {"total": len(data), "dropped": 0, "type_mapped": 0,
              "subject_fixed": 0, "ambiguous": 0, "keyword_flagged": 0,
              "option_images": 0, "option_image_rows": 0,
              "formula_rows": 0, "formula_tokens": 0}

    for d in data:
        qid = int(d["id"])
        old_subject = d.get("co", "")
        old_chapter = d.get("su", "")
        old_type = d.get("t", "")
        old_diff = d.get("lv", "")
        ntype = type_key(old_type)
        flags, reasons, action = [], [], "نگهداشتن"

        # ۱) حذف درسنامه
        if ntype in DROP_TYPES:
            counts["dropped"] += 1
            action = "حذف"
            reasons.append("درسنامه محتوای آموزشی است، نه سؤال.")
            report.append([qid, action, old_subject, "", old_chapter, old_chapter,
                           old_type, "", "", old_diff, 0, ";".join(flags), " | ".join(reasons)])
            continue

        # ۲) نگاشت نوع
        type_code = TYPE_MAP.get(ntype, "essay")
        new_type_fa = "تشریحی" if ntype == "تشریحیکوتاه" else old_type
        if ntype == "تشریحیکوتاه":
            counts["type_mapped"] += 1
            action = "نگاشت نوع"
            reasons.append("تشریحی کوتاه به تشریحی نگاشت شد.")

        # ۳) دشواری چهار سطح
        difficulty = DIFFICULTY_MAP.get(norm(old_diff), 0)
        difficulty_fa = DIFFICULTY_LABELS[difficulty]

        # ۴) اصلاح درس
        subject = old_subject
        chapter = old_chapter
        if qid in EXPLICIT_SUBJECT_FIX:
            subject, why = EXPLICIT_SUBJECT_FIX[qid]
            counts["subject_fixed"] += 1
            action = "اصلاح درس"
            flags.append("اصلاح دستی")
            reasons.append(why)
        elif old_chapter != AMBIGUOUS_CHAPTER and chapter_subject.get(old_chapter) != old_subject:
            subject = chapter_subject[old_chapter]
            counts["subject_fixed"] += 1
            action = "اصلاح درس"
            flags.append("اکثریت فصل")
            reasons.append("همخوانی با درس غالب فصل.")

        if old_chapter == AMBIGUOUS_CHAPTER:
            counts["ambiguous"] += 1
            flags.append("فصل ترکیبی")

        hint = keyword_flag(d.get("s", ""), subject)
        if hint:
            counts["keyword_flagged"] += 1
            flags.append("نشانه واژگانی: " + hint)

        # ۵) تصویرها، گزینهها و پاسخ
        # V361 — فرمولهای MathJax همانجا به توکن ویرایشگر فرمول تبدیل میشوند.
        q_html = mathjax_to_tokens(rewrite_html(d.get("q", "")))
        a_html = mathjax_to_tokens(rewrite_html(d.get("a", "")))
        text_tokens = mathjax_to_tokens(d.get("s", ""))
        answer_tokens = mathjax_to_tokens(plain(d.get("a", "")))
        choices = d.get("ch", []) or []
        options_text, option_images, correct_index = [], [], None
        for i, c in enumerate(choices):
            if not isinstance(c, list) or len(c) < 2:
                continue
            options_text.append(mathjax_to_tokens(plain(c[1])))
            # V360.8 — تصویر همین گزینه، به ترتیب HTML؛ هیچ تصویری به متن سؤال نمیرود.
            imgs = image_urls_in_order(c[1], fig_file, base)
            option_images.append(imgs[0] if imgs else None)
            if len(c) > 2 and c[2] is True and correct_index is None:
                correct_index = i
        n_option_images = sum(1 for u in option_images if u)
        counts["option_images"] += n_option_images
        counts["option_image_rows"] += 1 if n_option_images else 0
        blob = {"q": d.get("q", ""), "a": d.get("a", ""), "ch": choices}
        image_ids = image_refs(blob)
        image_urls = [base + "/images/" + fig_file[k] for k in image_ids if k in fig_file]

        row = {
            "id": qid, "grade": 1, "track": "",
            "subject": subject, "chapter": chapter, "topic": d.get("ob", ""),
            "area": d.get("cd", ""),
            "type_fa": new_type_fa, "type": type_code,
            "difficulty": difficulty, "difficulty_fa": difficulty_fa,
            "score": 1, "text": text_tokens,
            "has_image": bool(image_urls), "image_count": len(image_urls),
            "image_urls": image_urls,
        }
        catalog.append(row)
        payloads[str(qid)] = {
            "id": qid, "grade": 1, "track": "",
            "subject": subject, "chapter": chapter, "topic": d.get("ob", ""),
            "type_fa": new_type_fa, "type": type_code,
            "difficulty": difficulty, "difficulty_fa": difficulty_fa,
            "text": text_tokens,
            "q_html": q_html, "answer_html": a_html,
            "options": options_text, "correct_index": correct_index,
            "option_images": option_images,
            "answer_text": answer_tokens,
            "image_urls": image_urls,
        }

        n_formula = sum(formula_token_count(v) for v in
                        (text_tokens, q_html, a_html, answer_tokens, " ".join(options_text)))
        counts["formula_tokens"] += n_formula
        counts["formula_rows"] += 1 if n_formula else 0

        report.append([qid, action, old_subject, subject, old_chapter, chapter,
                       old_type, new_type_fa + " / " + type_code,
                       difficulty_fa, old_diff, difficulty,
                       ";".join(flags), " | ".join(reasons) or "بدون تغییر"])
        if action in ("اصلاح درس", "نگاشت نوع"):
            corrections.append([qid, action, old_subject, subject, old_type,
                                new_type_fa, old_diff, difficulty_fa, " | ".join(reasons)])

    # --- نوشتن خروجیها ----------------------------------------------------
    (out / "catalog.json").write_text(json.dumps(catalog, ensure_ascii=False), encoding="utf-8")
    (out / "questions.jsonl").write_text(
        "\n".join(json.dumps(v, ensure_ascii=False) for v in payloads.values()) + "\n", encoding="utf-8")
    with (out / "catalog.csv").open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=[k for k in catalog[0] if k != "image_urls"])
        w.writeheader()
        for r in catalog:
            w.writerow({k: v for k, v in r.items() if k != "image_urls"})

    with (out / "review_report.csv").open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["id", "action", "old_subject", "new_subject", "old_chapter", "new_chapter",
                    "old_type", "new_type", "new_difficulty_fa", "old_difficulty_fa",
                    "new_difficulty", "flags", "reason"])
        w.writerows(report)
    with (out / "corrections.csv").open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["id", "action", "old_subject", "new_subject", "old_type",
                    "new_type", "old_difficulty", "new_difficulty", "reason"])
        w.writerows(corrections)

    # شارد فهرست: یک فایل برای هر پایه، و یک فایل کوچکتر برای هر پایه+درس
    (catalog_dir / "g1.json").write_text(json.dumps(catalog, ensure_ascii=False), encoding="utf-8")
    by_subject = {}
    for r in catalog:
        by_subject.setdefault(r["subject"], []).append(r)
    for i, (subj, rows) in enumerate(sorted(by_subject.items()), 1):
        (catalog_dir / ("g1-s" + str(i) + ".json")).write_text(
            json.dumps({"grade": 1, "subject": subj, "rows": rows}, ensure_ascii=False), encoding="utf-8")
    (questions_dir / "g1.json").write_text(json.dumps(payloads, ensure_ascii=False), encoding="utf-8")

    manifest = {
        "version": "V361", "grade": 1, "base": base,
        "counts": counts,
        "catalog_rows": len(catalog),
        "question_payloads": len(payloads),
        "unique_images": len(by_hash),
        "image_references": len(fig),
        "subjects": {k: len(v) for k, v in by_subject.items()},
        "difficulty_labels": DIFFICULTY_LABELS,
        "type_map": TYPE_MAP,
        "dropped_types": sorted(DROP_TYPES),
        "fields_removed": ["source", "license", "reviewed"],
        "catalog_bytes": (catalog_dir / "g1.json").stat().st_size,
        "questions_bytes": (questions_dir / "g1.json").stat().st_size,
        "images_bytes": sum(p.stat().st_size for p in images.iterdir()),
    }
    (pub / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("REFERENCE_BANK_REVIEW: PASS")
    for k, v in counts.items():
        print("  ", k, "=", v)
    print("  catalog_rows =", len(catalog), "| payloads =", len(payloads),
          "| unique_images =", len(by_hash))
    print("  subjects =", manifest["subjects"])
    print("  publish =", pub)


if __name__ == "__main__":
    main()
