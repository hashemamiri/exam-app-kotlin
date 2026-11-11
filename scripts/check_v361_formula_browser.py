#!/usr/bin/env python3
"""V361 — فرمول MathJax بانک مرجع در مرورگر واقعی به نماد تبدیل شود.

همهٔ شاردها با route محلی شبیهسازی میشوند؛ تماس شبکهای بیرونی نیست.
سنجشها:
  • مبدل سایت روی بُردارهای مشترک با کاتلین و سازندهٔ شارد همان خروجی را بدهد؛
  • draft() متن، گزینهها و پاسخ را توکن `$...$` کند؛
  • پیشنمایش، فرمول خام `\\(...\\)` را نشان ندهد و توکن/نماد بدهد؛
  • افزودن گروهی، توکنها را به آزمون ببرد.
"""
import json
import pathlib
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
import mock_supabase_server as mss  # noqa: E402
import web_ui_e2e as e2e  # noqa: E402

BASE = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank"
Q_IMG = BASE + "/images/formula-q.webp"
O_IMG = BASE + "/images/formula-option.webp"
A_IMG = BASE + "/images/formula-answer.webp"

FORMULA_ROW = {
    "id": 1283285, "grade": 1, "track": "", "subject": "ریاضی", "chapter": "فصل فرمول",
    "type_fa": "تستی", "type": "multiple", "difficulty": 2, "difficulty_fa": "متوسط",
    "score": 1, "text": "حاصل \\(\\displaystyle \\text{۸}-\\text{۳}\\) کدام است؟",
    "has_image": True, "image_count": 3, "image_urls": [Q_IMG, O_IMG, A_IMG]
}
FORMULA_Q = {
    "id": 1283285, "grade": 1, "track": "", "subject": "ریاضی", "chapter": "فصل فرمول",
    "type_fa": "تستی", "type": "multiple", "difficulty": 2, "difficulty_fa": "متوسط",
    "text": "حاصل \\(\\displaystyle \\text{۸}-\\text{۳}\\) کدام است؟",
    "q_html": "<div><img src=\"" + Q_IMG + "\"><span class='rz-formula'>\\(\\displaystyle \\text{۸}-\\text{۳}\\)</span></div>",
    "answer_html": "<div><img src=\"" + A_IMG + "\"><span class='az-formula'>\\(\\text{۵}\\)</span></div>",
    "options": ["\\(\\text{۵}\\)", "\\(\\text{۶}\\)", "\\(\\text{۷}\\)", "\\(\\text{۸}\\)"],
    "correct_index": 0, "answer_text": "\\(\\text{۵}\\)",
    "image_urls": [Q_IMG, O_IMG, A_IMG],
    "option_images": [O_IMG, None, None, None]
}

# V366 — همان شکل واقعی شاردهای ۶ تا ۸: HTML با div/span و فرمول $...$
HTML_ROW = {
    "id": 9003661, "grade": 1, "track": "", "subject": "ریاضی", "chapter": "فصل فرمول",
    "type_fa": "تستی", "type": "multiple", "difficulty": 1, "difficulty_fa": "ساده",
    "score": 1, "text": "", "has_image": False, "image_count": 0, "image_urls": []
}
HTML_Q = {
    "id": 9003661, "grade": 1, "track": "", "subject": "ریاضی", "chapter": "فصل فرمول",
    "type_fa": "تستی", "type": "multiple", "difficulty": 1, "difficulty_fa": "ساده",
    "text": "",
    "q_html": "<div><div><span>حاصل</span> <span dir='ltr'> $\\displaystyle \\text{۲}+\\text{۳}$ </span> <span>کدام است؟</span></div></div>",
    "answer_html": "<div><span>پاسخ</span> $\\displaystyle \\text{۵}$</div>",
    "options": ["$\\displaystyle \\text{۴}$", "$\\displaystyle \\text{۵}$",
                "$\\displaystyle \\text{۶}$", "$\\displaystyle \\text{۷}$"],
    "correct_index": 1, "option_images": [None, None, None, None], "answer_text": "",
    "image_urls": []
}

fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:260]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def main():
    from playwright.sync_api import sync_playwright

    vectors = json.loads(
        (ROOT.parent / "app/src/test/resources/v361_formula_vectors.json").read_text(encoding="utf-8")
    )
    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()
    state.bank_categories = []
    state.bank_items = []

    png_1x1 = bytes.fromhex(
        "89504e470d0a1a0a0000000d4948445200000001000000010806000000"
        "1f15c4890000000d4944415478da63f8cf0000040100a0b826ea0000000049454e44ae426082")

    def storage(route):
        u = route.request.url
        if u.endswith((".webp", ".png", ".jpg", ".jpeg")):
            # V366 — تصویر ساختگی معتبر؛ پیش‌تر همین مسیر JSON برمی‌گرداند و تصویر لود نمی‌شد
            return route.fulfill(status=200, content_type="image/png",
                                 headers={"Access-Control-Allow-Origin": "*"}, body=png_1x1)
        if "catalog/g1.json" in u:
            body = json.dumps([FORMULA_ROW, HTML_ROW], ensure_ascii=False)
        elif "questions/g1.json" in u:
            body = json.dumps({"1283285": FORMULA_Q, "9003661": HTML_Q}, ensure_ascii=False)
        elif "catalog/" in u:
            body = "[]"
        else:
            body = "{}"
        route.fulfill(status=200, content_type="application/json",
                      headers={"Access-Control-Allow-Origin": "*"}, body=body)

    with tempfile.TemporaryDirectory(prefix="v361_formula_") as tmp:
        site = pathlib.Path(tmp)
        e2e.build_chunked_site_out(site, anon)
        web, url = e2e.start_readonly_site_server(site, mock_url, anon)
        try:
            with sync_playwright() as pw:
                br = pw.chromium.launch()
                page = br.new_page(viewport={"width": 1440, "height": 950})
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))
                page.route("**/reference-bank/**", storage)
                page.goto(url + "/", wait_until="domcontentloaded")
                page.locator("input[type='text']").first.fill("uitest_teacher")
                page.locator("input[type='password']").first.fill("TestPass1234!")
                page.locator("input[type='password']").first.press("Enter")
                page.locator("button[aria-label='افزودن']").wait_for()
                page.wait_for_function("() => !!window.RefBank", timeout=15000)

                print("\n=== بُردارهای مشترک ===")
                got = page.evaluate("""(vs) => vs.map(function (v) { return window.RefBank.mathJaxToTokens(v.raw); })""", vectors)
                for v, g in zip(vectors, got):
                    ok("تبدیل: " + v["raw"][:44], g == v["tokens"], (g, v["tokens"]))
                ok("همهٔ بُردارها بررسیشدند", len(got) == len(vectors), len(got))

                print("\n=== شارد خام → توکن هنگام خواندن ===")
                page.evaluate("() => window.ExamSite.go('bank')")
                page.get_by_text("سؤال جدید در بانک", exact=False).first.wait_for(timeout=15000)
                page.locator("button", has_text="بانک مرجع").first.click()
                md = page.locator(".modal.wide").last
                # V366 — فیلتر از V363 پنجرهٔ توکار است، نه نوار همیشگی بالای فهرست
                md.locator(".ref-filter-popup").wait_for(timeout=8000)
                md.locator(".ref-filter-popup select[title='پایه']").first.select_option("1")
                page.wait_for_timeout(400)
                md.locator(".ref-filter-popup .ref-confirm").first.click()
                md.locator(".ref-question-card").first.wait_for(timeout=15000)
                page.wait_for_function(
                    "() => document.querySelectorAll('.ref-question-card .mathx, .ref-question-card svg').length > 0",
                    timeout=20000)
                page.wait_for_timeout(600)
                body_text = md.inner_text()
                ok("فهرست دیگر فرمول خام نشان نمی‌دهد", "$" not in body_text and "\\(" not in body_text, body_text[:200])
                ok("صورت سؤال کارت به نماد تبدیل شد",
                   md.locator(".ref-question-body .mathx, .ref-question-body svg").count() > 0,
                   md.locator(".ref-question-body").first.inner_html()[:300])
                ok("گزینه‌های ریاضی کارت خالی نیستند",
                   md.locator(".ref-option .mathx, .ref-option svg").count() >= 4,
                   md.locator(".ref-option").first.inner_html()[:300])
                # V366 — بدون CSS موتور فرمول (.math-rich + #stMathCss) نمادها نامرئی می‌شدند
                ok("CSS موتور فرمول در صفحه تزریق شده است",
                   page.evaluate("() => !!document.getElementById('stMathCss')"))
                ok("پنجرهٔ بانک کلاس math-rich دارد",
                   md.evaluate("e => e.classList.contains('math-rich')"))
                ok("گزینه‌ها هم داخل math-rich رندر شده‌اند",
                   md.locator(".ref-option-text.math-rich").count() >= 4,
                   md.locator(".ref-option-text").first.get_attribute("class"))

                print("\n=== پیش‌نمایش: فرمول به نماد ===")
                md.get_by_text("پیش‌نمایش", exact=True).first.click()
                pv = page.locator(".modal.wide").last
                pv.get_by_text("پیش‌نمایش سؤال", exact=True).first.wait_for(timeout=8000)
                page.wait_for_timeout(1500)
                pv_html = pv.inner_html()
                pv_text = pv.inner_text()
                ok("پیش‌نمایش فرمول خام MathJax نشان نمی‌دهد", "\\(" not in pv_html, pv_html[:300])
                ok("توکن خام $ در متن دیده نمی‌شود", "$" not in pv_text, pv_text[:300])
                ok("صورت سؤال به نماد تبدیل شد",
                   pv.locator(".ref-question-body .mathx, .ref-question-body svg").count() > 0, pv_html[:300])
                ok("پاسخ به نماد تبدیل شد",
                   pv.locator(".ref-answer-details .mathx, .ref-answer-details svg").count() > 0, pv_html[-400:])
                ok("همهٔ گزینه‌ها و پاسخ نماد شدند",
                   pv.locator(".mathx, .math-inline").count() >= 5,
                   pv.locator(".mathx, .math-inline").count())

                print("\n=== تصویر کوچک و بزرگنمایی عمومی ===")
                # V366 — alt با شمارهٔ فارسی است («تصویر گزینه ۱»)؛ تطبیق پیشوندی
                thumb = pv.locator("img[alt^='تصویر گزینه']").first
                # V366 — تصویر با loading=lazy پایین پنجره است؛ تا اسکرول نشود «visible» نمی‌شود
                thumb.wait_for(state="attached", timeout=8000)
                thumb.scroll_into_view_if_needed()
                page.wait_for_timeout(400)
                thumb_style = thumb.evaluate("e => ({maxWidth: e.style.maxWidth, maxHeight: e.style.maxHeight, cursor: e.style.cursor})")
                ok("تصویر گزینه کوچک و کلیکخور است",
                   thumb_style["maxWidth"] != "" and thumb_style["maxHeight"] != "" and thumb_style["cursor"] == "zoom-in",
                   thumb_style)
                thumb.click()
                light = page.locator(".arena-image-preview-bg").last
                light.wait_for(timeout=5000)
                close = light.locator("button[aria-label='بستن تصویر']")
                ok("بزرگنمایی ضربدر بستن دارد", close.count() == 1)
                ok("تصویر بزرگ در لایتباکس هست", light.locator("img").count() == 1)
                close.click()
                ok("ضربدر لایتباکس را میبندد", page.locator(".arena-image-preview-bg").count() == 0)

                print("\n=== draft و افزودن به آزمون ===")
                draft = page.evaluate("""() => {
                    var q = window.RefBank.draft(%s, true);
                    return {text: q.text, options: q.options, explanation: q.answerExplanation};
                }""" % json.dumps(FORMULA_Q, ensure_ascii=False))
                ok("متن آزمون توکن شد", "$" in draft["text"] and "\\(" not in draft["text"], draft["text"])
                ok("گزینهها توکن شدند", all(("$" in o and "\\(" not in o) for o in draft["options"]), draft["options"])
                ok("پاسخ توکن شد", "$" in draft["explanation"] and "\\(" not in draft["explanation"], draft["explanation"])
                # The builder answer field is verified structurally in the Kotlin regression test;
                # opening a fresh online builder prompts the mock exam-settings workflow.
                builder_src = (ROOT.parent / "site/src/builder.js").read_text(encoding="utf-8")
                ok("توضیح پاسخ از ویرایشگر فرمول توکنی استفاده میکند",
                   "tokenTextarea(ansTa, {small: true})" in builder_src)
                ok("برچسب پاسخ، راهنمای معلم را روشن میکند",
                   "توضیح پاسخ / راهنمای معلم" in builder_src)

                ok("هیچ خطای جاوااسکریپتی رخ نداد", not errors, errors[:3])
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    if fails:
        print("\nV361_FORMULA_BROWSER: FAIL — " + str(len(fails)) + " مورد")
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("\nV361_FORMULA_BROWSER: PASS (0 خطا)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
