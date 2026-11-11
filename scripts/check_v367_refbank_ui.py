#!/usr/bin/env python3
# -*- coding: utf-8 -*-
'''V367 — نگهبان مرورگری پنجرهٔ بانک مرجع (سایت).

چه چیزی را می‌سنجد:
  ۱) برچسب‌های کشویی با نیم‌فاصله‌اند: «همهٔ پایه‌ها»، «همهٔ درس‌ها»، «همهٔ فصل‌ها»،
     «همهٔ نوع‌ها»، «همهٔ سطح‌ها» و شمارش «انتخاب‌شده».
  ۲) در فهرست، همهٔ ابزارها در یک ردیف هدر و آن هدر ثابت است.
  ۳) آیکن جست‌وجو باکس جست‌وجو را در ردیف پایین‌تر باز می‌کند.
  ۴) هیچ خطای جاوااسکریپتی رخ نمی‌دهد.

اجرا: python3 scripts/check_v367_refbank_ui.py
'''
import json
import pathlib
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
import mock_supabase_server as mss  # noqa: E402
import web_ui_e2e as e2e  # noqa: E402

Z = "\u200c"
PNG_1X1 = bytes.fromhex(
    "89504e470d0a1a0a0000000d4948445200000001000000010806000000"
    "1f15c4890000000d4944415478da63f8cf0000040100a0b826ea0000000049454e44ae426082")

TEXT = "عبارت $\\displaystyle \\text{۲}^{\\text{۴}}\\times\\text{۳}^{\\text{۴}}$ با کدام گزینه برابر است؟"
ROWS = [
    {"id": 1281038, "grade": 1, "track": "", "subject": "ریاضی",
     "chapter": "سوالات و مطالب ترکیبی و کلی", "topic": "", "area": "",
     "type_fa": "تستی", "type": "multiple", "difficulty": 4, "difficulty_fa": "خیلی دشوار",
     "score": 1, "text": TEXT, "has_image": False, "image_count": 0, "image_urls": []},
    {"id": 1281039, "grade": 1, "track": "", "subject": "ریاضی", "chapter": "عدد و رقم",
     "topic": "", "area": "", "type_fa": "تستی", "type": "multiple", "difficulty": 2,
     "difficulty_fa": "متوسط", "score": 1, "text": TEXT, "has_image": False,
     "image_count": 0, "image_urls": []},
]
QS = {str(r["id"]): {"id": r["id"], "grade": 1, "subject": r["subject"], "type_fa": "تستی",
                     "type": "multiple", "difficulty": r["difficulty"],
                     "difficulty_fa": r["difficulty_fa"], "text": TEXT,
                     "q_html": "<div>" + TEXT + "</div>",
                     "answer_html": "<div>$\\displaystyle \\text{۶}^{\\text{۶}}$</div>",
                     "answer_text": "۶^۶",
                     "options": ["$\\text{۶}^{\\text{۶}}$", "$\\text{۵}^{\\text{۵}}$",
                                 "$\\text{۴}^{\\text{۴}}$", "$\\text{۳}^{\\text{۳}}$"],
                     "correct_index": 0, "image_urls": [],
                     "option_images": [None, None, None, None]} for r in ROWS}

fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:260]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def main():
    from playwright.sync_api import sync_playwright

    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()
    state.bank_categories = []
    state.bank_items = []

    def storage(route):
        u = route.request.url
        if u.endswith((".webp", ".png", ".jpg", ".jpeg")):
            return route.fulfill(status=200, content_type="image/png",
                                 headers={"Access-Control-Allow-Origin": "*"}, body=PNG_1X1)
        if "catalog/g1.json" in u:
            body = json.dumps(ROWS, ensure_ascii=False)
        elif "questions/g1.json" in u:
            body = json.dumps(QS, ensure_ascii=False)
        elif "catalog/" in u:
            body = "[]"
        else:
            body = "{}"
        route.fulfill(status=200, content_type="application/json",
                      headers={"Access-Control-Allow-Origin": "*"}, body=body)

    with tempfile.TemporaryDirectory(prefix="v367_ui_") as tmp:
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
                page.evaluate("() => window.ExamSite.go('bank')")
                page.get_by_text("سؤال جدید در بانک", exact=False).first.wait_for(timeout=15000)
                page.locator("button", has_text="بانک مرجع").first.click()
                md = page.locator(".modal.wide").last

                print("\n=== پنجرهٔ فیلتر: نیم‌فاصله ===")
                md.locator(".ref-filter-popup").wait_for(timeout=8000)
                for title, want in [("پایه", "همهٔ پایه" + Z + "ها"),
                                    ("درس", "همهٔ درس" + Z + "ها"),
                                    ("فصل", "همهٔ فصل" + Z + "ها"),
                                    ("نوع سؤال", "همهٔ نوع" + Z + "ها"),
                                    ("دشواری", "همهٔ سطح" + Z + "ها")]:
                    opts = md.locator(".ref-filter-popup select[title='%s'] option" % title).all_inner_texts()
                    ok("برچسب «%s» با نیم‌فاصله" % want, want in opts, opts)
                ok("هیچ برچسب بدون نیم‌فاصله نمانده",
                   not any(" ها" in o for o in md.locator(".ref-filter-popup option").all_inner_texts()),
                   md.locator(".ref-filter-popup option").all_inner_texts())

                md.locator(".ref-filter-popup select[title='پایه']").first.select_option("1")
                page.wait_for_timeout(500)
                md.locator(".ref-filter-popup .ref-confirm").first.click()
                md.locator(".ref-question-card").first.wait_for(timeout=15000)
                page.wait_for_timeout(1200)

                print("\n=== هدر فهرست: یک ردیف و ثابت ===")
                row = md.locator(".refbank-one-row")
                ok("ردیف ابزارها وجود دارد (.refbank-one-row)", row.count() == 1, row.count())
                box = row.evaluate('''e => {
                    const kids = Array.from(e.children).filter(k => k.offsetParent !== null);
                    const ys = kids.map(k => { const r = k.getBoundingClientRect(); return Math.round(r.top + r.height / 2); });
                    const hdr = e.closest('.refbank-header') || e;
                    return {count: kids.length, spread: Math.max(...ys) - Math.min(...ys),
                            sticky: getComputedStyle(hdr).position};
                }''')
                ok("همهٔ ابزارها هم‌تراز یک ردیف‌اند (اختلاف محور ≤ 6px)", box["spread"] <= 6, box)
                ok("هدر ثابت است (sticky)", box["sticky"] == "sticky", box["sticky"])
                ok("عنوان + ۸ ابزار + بستن در ردیف‌اند", box["count"] == 10, box["count"])
                txt = row.inner_text()
                ok("شمارش با «انتخاب‌شده»", "انتخاب" + Z + "شده" in txt, txt)
                ok("شمارش، افزودن پاسخ و افزودن گروهی در ردیف‌اند",
                   all(w in txt for w in ["سؤال · صفحهٔ", "افزودن پاسخ", "افزودن گروهی"]), txt)
                titles = row.evaluate("e => Array.from(e.querySelectorAll('[title]')).map(x => x.getAttribute('title'))")
                ok("دکمه‌های صفحه، جست‌وجو، فیلتر و بستن در ردیف‌اند",
                   all(t in titles for t in ["انتخاب/برداشتن صفحه", "قبلی", "بعدی", "جست‌وجو",
                                             "فیلتر", "بستن بانک سؤال مرجع"]), titles)
                ok("دکمهٔ «پاک کردن انتخاب» وجود ندارد", "پاک کردن انتخاب" not in md.inner_text())

                print("\n=== جست‌وجو: باکس در ردیف پایین‌تر ===")
                row.locator("[title='جست‌وجو']").click()
                page.wait_for_timeout(500)
                ok("باکس جست‌وجو باز شد", md.locator(".refbank-search-row .refbank-search-input").count() == 1)
                pos = md.locator(".refbank-search-row .refbank-search-input").first.evaluate(
                    "e => Math.round(e.getBoundingClientRect().top)")
                posr = row.evaluate("e => Math.round(e.getBoundingClientRect().bottom)")
                ok("باکس زیر ردیف هدر است", pos >= posr - 2, (pos, posr))

                print("\n=== متای کارت ===")
                meta = md.locator(".ref-card-meta").first.inner_text()
                ok("ردیف متا شامل پایه/درس/فصل/نوع/دشواری",
                   all(w in meta for w in ["پایه ۱", "ریاضی", "تستی", "دشوار"]), meta)

                print("\n=== خطای جاوااسکریپت ===")
                ok("هیچ خطای جاوااسکریپتی رخ نداد", not errors, errors[:2])
                br.close()
        finally:
            web.shutdown()

    print()
    if fails:
        print("V367_REFBANK_UI: FAIL (%d خطا)" % len(fails))
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("V367_REFBANK_UI: PASS (0 خطا)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
