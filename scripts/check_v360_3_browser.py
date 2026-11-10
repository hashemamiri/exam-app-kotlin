#!/usr/bin/env python3
"""V360.5 — بانک مرجع در مرورگر واقعی: کشوها، پیشفرض «همه»، افزودن گروهی، پاسخ جدا.

شاردهای آروان در این تست با route محلی شبیهسازی میشوند؛ هیچ تماس شبکهای
بیرونی لازم نیست. سنجشها:
  • منوی + سایت پنج مورد قدیمی را دارد و «بانک مرجع» در آن نیست.
  • پنجرهٔ فیلتر همه کشویی و پیشفرض «همه» است؛ درس تا پیش از پایه فقط «همه»،
    ولی نوع سؤال و دشواری بدون پایه هم همهٔ گزینهها را دارند.
  • رشته فقط برای پایهٔ ۱۰ به بعد میآید.
  • بستن فیلتر → فهرست سؤالها → انتخاب گروهی → آزمون تازه.
  • تصویر پاسخ هرگز وارد images سؤال نمیشود و پاسخ فقط در کلید پاسخ است.
  • سازندهٔ آزمون همچنان بانک مرجع را باز میکند.
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
Q_IMG = BASE + "/images/q.webp"
A_IMG = BASE + "/images/a.webp"
O1_IMG = BASE + "/images/o1.webp"
O2_IMG = BASE + "/images/o2.webp"

fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:300]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def catalog():
    def row(i, subject, chapter, diff, qtext):
        return {
            "id": i, "grade": 1, "track": "", "subject": subject, "chapter": chapter,
            "type_fa": "تستی", "type": "multiple", "difficulty": diff,
            "difficulty_fa": ["تعییننشده", "ساده", "متوسط", "دشوار", "خیلی دشوار"][diff],
            "score": 1, "text": qtext, "has_image": True, "image_count": 2,
            "image_urls": [Q_IMG, O1_IMG, O2_IMG, A_IMG]
        }
    return [row(1, "ریاضی", "فصل یک", 1, "پرسش ریاضی یک"),
            row(2, "فارسی", "فصل دو", 2, "پرسش فارسی دو")]


def question(i, subject, chapter):
    return {
        "id": i, "grade": 1, "track": "", "subject": subject, "chapter": chapter,
        "type_fa": "تستی", "type": "multiple", "difficulty": 1, "difficulty_fa": "ساده",
        "text": "پرسش " + str(i), "q_html": "<div>صورت سؤال <img src=\"" + Q_IMG + "\"></div>",
        "answer_html": "<div>پاسخ درست <img src=\"" + A_IMG + "\"></div>",
        "options": ["الف", "ب", "ج", "د"], "correct_index": 1,
        "option_images": [O1_IMG, O2_IMG, None, None],
        "answer_text": "گزینهٔ ب درست است", "image_urls": [Q_IMG, O1_IMG, O2_IMG, A_IMG]
    }


def main():
    from playwright.sync_api import sync_playwright

    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()
    state.bank_categories = []
    state.bank_items = []

    rows = catalog()
    qs = {"1": question(1, "ریاضی", "فصل یک"), "2": question(2, "فارسی", "فصل دو")}

    with tempfile.TemporaryDirectory(prefix="v360_5_site_") as tmp:
        site = pathlib.Path(tmp)
        e2e.build_chunked_site_out(site, anon)
        web, url = e2e.start_readonly_site_server(site, mock_url, anon)
        try:
            with sync_playwright() as pw:
                br = pw.chromium.launch()
                page = br.new_page(viewport={"width": 1440, "height": 950})
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))

                def storage(route):
                    u = route.request.url
                    if "catalog/g1.json" in u:
                        body = json.dumps(rows, ensure_ascii=False)
                    elif "questions/g1.json" in u:
                        body = json.dumps(qs, ensure_ascii=False)
                    elif "catalog/" in u:
                        body = "[]"
                    else:
                        body = "{}"
                    route.fulfill(status=200, content_type="application/json",
                                  headers={"Access-Control-Allow-Origin": "*"}, body=body)

                page.route("**/reference-bank/**", storage)
                page.goto(url + "/", wait_until="domcontentloaded")
                page.locator("input[type='text']").first.fill("uitest_teacher")
                page.locator("input[type='password']").first.fill("TestPass1234!")
                page.locator("input[type='password']").first.press("Enter")
                page.locator("button[aria-label='افزودن']").wait_for()
                page.wait_for_function("() => !!window.RefBank", timeout=15000)

                print("\n=== منوی + : پنج مورد قدیمی، بدون بانک مرجع ===")
                page.locator("button[aria-label='افزودن']").first.click()
                page.locator(".m-radial-item").first.wait_for(timeout=8000)
                labels = page.locator(".m-radial-item .l").all_text_contents()
                ok("پنج کارت قدیمی دیده میشود", len(labels) == 5, labels)
                ok("آزمون چاپی جدید هست", any("آزمون چاپی جدید" in x for x in labels), labels)
                ok("بانک مرجع در + نیست", not any("بانک مرجع" in x for x in labels), labels)
                page.evaluate("() => { var b = document.querySelector('.m-radial-bg'); if (b) b.remove(); }")

                print("\n=== پنجرهٔ فیلتر: همه کشویی و پیشفرض «همه» ===")
                page.evaluate("() => window.ExamSite.go('bank')")
                page.get_by_text("سؤال جدید در بانک", exact=False).first.wait_for(timeout=15000)
                page.locator("button", has_text="بانک مرجع").first.click()
                md = page.locator(".modal.wide").last
                md.get_by_text("بستن فیلتر و نمایش سؤالها", exact=True).first.wait_for(timeout=8000)
                selects = md.locator("select")
                ok("همهٔ فیلترها کشوییاند", selects.count() >= 5, selects.count())
                def sel(title):
                    return md.locator("select[title='" + title + "']").first

                ok("پایه پیشفرض «همه» است", sel("پایه").input_value() == "0", sel("پایه").input_value())
                ok("درس پیشفرض «همه» است", sel("درس").input_value() == "", sel("درس").input_value())
                ok("نوع سؤال پیشفرض «همه» است", sel("نوع سؤال").input_value() == "")
                ok("دشواری پیشفرض «همه» است", sel("دشواری").input_value() == "")
                ok("تا پیش از پایه، درس فقط «همه» دارد", sel("درس").locator("option").count() == 1,
                   sel("درس").locator("option").count())
                ok("نوع سؤال بدون پایه همهٔ گزینهها را دارد", sel("نوع سؤال").locator("option").count() > 2,
                   sel("نوع سؤال").locator("option").count())
                ok("دشواری بدون پایه همهٔ گزینهها را دارد", sel("دشواری").locator("option").count() == 6,
                   sel("دشواری").locator("option").count())
                ok("پایهٔ ۱ رشته ندارد", md.locator("select[title='رشته']").count() == 0)

                print("\n=== انتخاب پایه و وابستگیها ===")
                sel("پایه").select_option("1")
                page.wait_for_function("""() => { var s = document.querySelector('.modal.wide select[title=\"درس\"]'); return s && s.options.length > 1; }""", timeout=10000)
                md = page.locator(".modal.wide").last
                sel2 = lambda t: md.locator("select[title='" + t + "']").first
                ok("با پایهٔ ۱ لیست درسها پر شد", sel2("درس").locator("option").count() > 1,
                   sel2("درس").locator("option").count())
                ok("پایهٔ ۱ همچنان رشته ندارد", md.locator("select[title='رشته']").count() == 0)
                sel2("پایه").select_option("10")
                page.wait_for_function("""() => !!document.querySelector('.modal.wide select[title=\"رشته\"]')""", timeout=10000)
                md = page.locator(".modal.wide").last
                ok("پایهٔ ۱۰ رشته دارد", md.locator("select[title='رشته']").count() == 1)
                sel2("پایه").select_option("1")
                page.wait_for_function("""() => !document.querySelector('.modal.wide select[title=\"رشته\"]')""", timeout=10000)
                md = page.locator(".modal.wide").last

                print("\n=== بستن فیلتر و افزودن گروهی ===")
                md.get_by_text("بستن فیلتر و نمایش سؤالها", exact=True).first.click()
                md.get_by_text("پرسش ریاضی یک", exact=False).first.wait_for(timeout=8000)
                ok("فهرست سؤالها پس از بستن فیلتر آمد", True)
                md.locator("button[title='بازگشت به فیلتر کشویی']").first.wait_for(timeout=5000)
                ok("آیکن فیلتر برای برگشت هست", True)
                md.get_by_text("انتخاب/برداشتن صفحه", exact=True).first.click()
                md.get_by_text("افزودن گروهی", exact=False).first.click()
                page.get_by_text("آزمون از بانک مرجع", exact=False).first.wait_for(timeout=12000, state="attached")
                ok("سؤالها گروهی به آزمون تازه افزوده شدند", True)

                print("\n=== پاسخ و تصویر پاسخ فقط در بخش پاسخ ===")
                sep = page.evaluate("""() => {
                    var p = window.RefBank.draft(%s, true);
                    var off = window.RefBank.draft(%s, false);
                    var q = window.SiteBuilder.decodeQuestion(p, p);
                    var enc = window.SiteBuilder.encodeQuestions([q]);
                    return {
                        images: p.images, answerImages: p.answerImages,
                        optionImages: p.optionImages,
                        explanation: p.answerExplanation, expected: p.expectedText,
                        offAnswerImages: off.answerImages, offExplanation: off.answerExplanation,
                        pub: enc.publicQuestions[0] || {}, key: enc.answerKey[0] || {}
                    };
                }""" % (json.dumps(qs["1"], ensure_ascii=False), json.dumps(qs["1"], ensure_ascii=False)))
                ok("تصویر سؤال فقط تصویر صورت است", sep["images"] == [Q_IMG], sep["images"])
                ok("تصویر پاسخ وارد سؤال نشد", A_IMG not in sep["images"], sep["images"])
                ok("تصویر گزینهها وارد متن سؤال نشد",
                   O1_IMG not in sep["images"] and O2_IMG not in sep["images"], sep["images"])
                ok("تصویر گزینهها کنار همان گزینههاست",
                   sep["optionImages"] == [O1_IMG, O2_IMG, None, None], sep["optionImages"])
                ok("تصویر پاسخ در بخش پاسخ است", sep["answerImages"] == [A_IMG], sep["answerImages"])
                ok("متن پاسخ در بخش پاسخ است", "پاسخ درست" in sep["explanation"], sep["explanation"])
                ok("متن پاسخ وارد پاسخ قابل قبول نشد", sep["expected"] == "", sep["expected"])
                ok("با خاموش کردن گزینه هیچ پاسخی کپی نمیشود",
                   sep["offAnswerImages"] == [] and sep["offExplanation"] == "", sep)
                ok("پاسخ در دادهٔ عمومی سؤال نیست",
                   "answerExplanation" not in sep["pub"] and "answerImages" not in sep["pub"],
                   sorted(sep["pub"].keys()))
                ok("پاسخ در کلید پاسخ سرور ذخیره میشود",
                   "پاسخ درست" in str(sep["key"].get("answerExplanation", "")) and
                   sep["key"].get("answerImages") == [A_IMG], sep["key"])

                print("\n=== مسیر آزمون جدید ===")
                page.evaluate("() => window.ExamSite.go('builder', null)")
                page.locator("button", has_text="بانک مرجع").first.wait_for(timeout=12000, state="attached")
                ok("دکمهٔ بانک مرجع در سازندهٔ آزمون هست", True)

                ok("هیچ خطای جاوااسکریپتی رخ نداد", not errors, errors[:3])
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    if fails:
        print("\nV360_3_BROWSER: FAIL — " + str(len(fails)) + " مورد")
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("\nV360_3_BROWSER: PASS (0 خطا)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
