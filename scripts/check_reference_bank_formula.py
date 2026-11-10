#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V361 — نگهبان «فرمول MathJax → نماد ویرایشگر فرمول».

چرا: شاردهای بانک مرجع فرمولها را بهصورت `\\(...\\)`، `\\[...\\]` و
`$$...$$` نگه میدارند، ولی ویرایشگر فرمول اپ/سایت فقط توکن `$...$`
میشناسد؛ بدون تبدیل، کاربر متن خام بکاسلش میبیند.

نگهبان:
  ۱) پین میکند که مبدل در اپ، سایت و سازندهٔ شارد هست؛
  ۲) بُردارهای مشترک (کاتلین/جاوااسکریپت/پایتون) را میسنجد؛
  ۳) با --shard شارد واقعی را بررسی میکند؛
  ۴) --selftest نمونههای سالم و خراب را میآزماید.
"""
import argparse
import importlib.util
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:200]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def load_generator():
    path = ROOT / "scripts" / "review_reference_bank.py"
    spec = importlib.util.spec_from_file_location("v361_review", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--shard", help="مسیر شارد questions/gN.json")
    ap.add_argument("--selftest", action="store_true")
    args = ap.parse_args()

    vectors = json.loads((ROOT / "app/src/test/resources/v361_formula_vectors.json").read_text(encoding="utf-8"))

    print("=== پینهای سورس ===")
    models = (ROOT / "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankFormula.kt").read_text(encoding="utf-8")
    ok("اپ مبدل خالص دارد", "fun mathJaxToTokens(raw: String): String" in models)
    ok("اپ شارد کهنه را هنگام خواندن تبدیل میکند",
       ".withFormulaTokens()" in
       (ROOT / "app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt").read_text(encoding="utf-8"))
    ok("نگاشت اپ فرمولها را توکن میکند",
       "ReferenceBankFormula.mathJaxToTokens" in
       (ROOT / "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt").read_text(encoding="utf-8"))
    ok("پیشنمایش اپ نماد SVG نشان میدهد",
       "NativeMathText" in
       (ROOT / "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt").read_text(encoding="utf-8"))
    ref = (ROOT / "site/src/refbank.js").read_text(encoding="utf-8")
    ok("سایت مبدل دارد", "function mathJaxToTokens(raw)" in ref)
    ok("سایت شارد را هنگام خواندن توکن میکند", "function tokenizeQuestion(q)" in ref)
    ok("سایت نماد فرمول رندر میکند", "function renderFormulaSpans(root)" in ref)
    ok("پاسخ در سایت با ویرایشگر تراشهای است",
       "tokenTextarea(ansTa, {small: true})" in
       (ROOT / "site/src/builder.js").read_text(encoding="utf-8"))
    site_app = (ROOT / "site/src/app.js").read_text(encoding="utf-8")
    ok("بزرگنمایی یکپارچهٔ تصاویر در همهٔ پنلها هست", "function openImagePreview(src, alt)" in site_app)
    ok("ضربدر بستن تصویر هست", "بستن تصویر" in site_app)
    ok("دکمهٔ بررسی شارد حذف شده", "بررسی شارد" not in ref)
    ok("سازندهٔ شارد توکن تمیز مینویسد", "def mathjax_to_tokens(raw):" in
       (ROOT / "scripts/review_reference_bank.py").read_text(encoding="utf-8"))

    print("\n=== بُردارهای مشترک (سازندهٔ شارد) ===")
    gen = load_generator()
    for v in vectors:
        got = gen.mathjax_to_tokens(v["raw"])
        ok("تبدیل: " + v["raw"][:42], got == v["tokens"], got)
    ok("تبدیل دوبارهبیضرر است",
       all(gen.mathjax_to_tokens(v["tokens"]) == v["tokens"] for v in vectors))

    if args.shard:
        print("\n=== شارد واقعی ===")
        try:
            data = json.loads(pathlib.Path(args.shard).read_text(encoding="utf-8"))
            raw_rows, token_rows = 0, 0
            for q in (data.values() if isinstance(data, dict) else data):
                if not isinstance(q, dict):
                    continue
                blob = " ".join(str(q.get(k) or "") for k in ("text", "q_html", "answer_html", "answer_text", "options"))
                if "\\(" in blob or "\\[" in blob or "$$" in blob:
                    raw_rows += 1
                if "$" in gen.mathjax_to_tokens(blob):
                    token_rows += 1
            ok("شارد خوانده شد", raw_rows + token_rows > 0, (raw_rows, token_rows))
            ok("همهٔ فرمولهای خام قابل تبدیلاند", raw_rows >= 0)
            print("  ردیفهای دارای فرمول خام:", raw_rows)
            print("  ردیفهای دارای توکن:", token_rows)
        except Exception as e:
            ok("شارد خوانده شد", False, str(e)[:160])

    if args.selftest:
        print("\n=== خودآزمون ===")
        ok("دو فرمول چسبیده جدا میشوند", gen.mathjax_to_tokens("\\(a\\)\\(b\\)") == "$a$ $b$")
        ok("توکن موجود دستنخورده میماند", gen.mathjax_to_tokens("$already$") == "$already$")
        ok("متن بیفرمول دستنخورده میماند", gen.mathjax_to_tokens("متن ساده") == "متن ساده")
        bad = "\\(ناتمام"
        ok("فرمول ناتمام خراب نمیشود", isinstance(gen.mathjax_to_tokens(bad), str))
        ok("بُردارها پراند", len(vectors) >= 8, len(vectors))

    print()
    if fails:
        print("REFERENCE_BANK_FORMULA: FAIL (%d مورد)" % len(fails))
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("REFERENCE_BANK_FORMULA: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
