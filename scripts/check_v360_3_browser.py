#!/usr/bin/env python3
"""V360.3 — بانک مرجع در مرورگر واقعی: فیلتر مرحلهای، افزودن گروهی، پاسخ جدا.

شاردهای آروان در این تست با route محلی شبیهسازی میشوند؛ هیچ تماس شبکهای
بیرونی لازم نیست. سنجشها:
  • رشته فقط برای پایهٔ ۱۰ به بعد میآید و پایهٔ ۱ مستقیم به درس میرود.
  • تأیید فیلتر → فهرست سؤالها → انتخاب گروهی → آزمون تازه با همان سؤالها.
  • تصویر پاسخ هرگز وارد images سؤال نمیشود و پاسخ فقط در کلید پاسخ است.
  • دکمهٔ + و سازندهٔ آزمون، هر دو بانک مرجع را باز میکنند.
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
            "image_urls": [Q_IMG, A_IMG]
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
        "answer_text": "گزینهٔ ب درست است", "image_urls": [Q_IMG, A_IMG]
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

    with tempfile.TemporaryDirectory(prefix="v360_3_site_") as tmp:
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

                print("\n=== دکمهٔ + : بانک مرجع در دسترس ===")
                page.locator("button[aria-label='افزودن']").first.click()
                page.get_by_text("بانک مرجع", exact=True).first.wait_for(timeout=8000)
                ok("کارت «بانک مرجع» در منوی + دیده میشود", True)
                page.keyboard.press("Escape")
                page.evaluate("() => { var b = document.querySelector('.m-radial-bg'); if (b) b.remove(); }")

                print("\n=== فیلتر مرحلهای: پایه ۱ رشته ندارد ===")
                page.evaluate("() => window.ExamSite.go('bank')")
                page.get_by_text("سؤال جدید در بانک", exact=False).first.wait_for(timeout=15000)
                page.locator("button", has_text="بانک مرجع").first.click()
                md = page.locator(".modal.wide").last
                md.get_by_text("پایه را تعیین کنید", exact=False).first.wait_for(timeout=8000)
                ok("پنجرهٔ فیلتر باز شد", True)
                md.get_by_text("پایهٔ ۱", exact=True).first.click()
                md.get_by_text("بعدی", exact=True).first.click()
                body = md.inner_text()
                ok("پایهٔ ۱ فیلد رشته ندارد", "رشته را انتخاب کنید" not in body, body[:200])
                ok("پایهٔ ۱ مستقیم لیست دروس را آورد", "۳) درس" in body, body[:200])

                print("\n=== فیلتر مرحلهای: پایه ۱۰ رشته دارد ===")
                page.evaluate("() => { var b = [...document.querySelectorAll('.modal-bg')].pop(); if (b) b.remove(); window.RefBank.open(); }")
                md10 = page.locator(".modal.wide").last
                md10.get_by_text("پایه را تعیین کنید", exact=False).first.wait_for(timeout=8000)
                md10.get_by_text("پایهٔ ۱۰", exact=True).first.click()
                md10.get_by_text("بعدی", exact=True).first.click()
                body10 = md10.inner_text()
                ok("پایهٔ ۱۰ فیلد رشته دارد", "رشته را انتخاب کنید" in body10, body10[:200])
                ok("فهرست دروس پایهٔ ۱۰ خالی نماند", "انسانی" in body10 or "همهٔ رشتهها" in body10, body10[:200])
                page.evaluate("() => { var b = [...document.querySelectorAll('.modal-bg')].pop(); if (b) b.remove(); }")

                print("\n=== تأیید فیلتر و افزودن گروهی ===")
                page.evaluate("() => window.RefBank.open()")
                md2 = page.locator(".modal.wide").last
                md2.get_by_text("پایه را تعیین کنید", exact=False).first.wait_for(timeout=8000)
                md2.get_by_text("پایهٔ ۱", exact=True).first.click()
                md2.get_by_text("بعدی", exact=True).first.click()
                md2.locator(".chip", has_text="ریاضی").first.click()
                for _ in range(4):
                    btn = md2.get_by_text("بعدی", exact=True).first
                    if btn.count():
                        btn.click()
                        md2 = page.locator(".modal.wide").last
                md2.get_by_text("تأیید فیلتر و نمایش سؤالها", exact=True).first.click()
                md2.get_by_text("پرسش ریاضی یک", exact=False).first.wait_for(timeout=8000)
                ok("فهرست سؤالها پس از تأیید فیلتر آمد", True)
                md2.get_by_text("انتخاب/برداشتن صفحه", exact=True).first.click()
                md2.get_by_text("افزودن گروهی", exact=False).first.click()
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
                        explanation: p.answerExplanation, expected: p.expectedText,
                        offAnswerImages: off.answerImages, offExplanation: off.answerExplanation,
                        pub: enc.publicQuestions[0] || {}, key: enc.answerKey[0] || {}
                    };
                }""" % (json.dumps(qs["1"], ensure_ascii=False), json.dumps(qs["1"], ensure_ascii=False)))
                ok("تصویر سؤال فقط تصویر صورت است", sep["images"] == [Q_IMG], sep["images"])
                ok("تصویر پاسخ وارد سؤال نشد", A_IMG not in sep["images"], sep["images"])
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
