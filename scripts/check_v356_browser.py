#!/usr/bin/env python3
"""V356 — بررسی هدفمند چک‌لیست تصحیح کد در مرورگر واقعی.

می‌سنجد که معلم موارد آزمون را تعریف می‌کند، چک‌لیست در تب تصحیح
ظاهر می‌شود، علامت‌زدن نتیجه پیشنهاد نمره را عوض می‌کند، و هیچ نمره‌ای
خودکار روی سرور ذخیره نمی‌شود. همچنین می‌سنجد که دانش‌آموز به پاسخ
مرجع و موردهای پنهان دسترسی ندارد.
"""
import pathlib
import sys
import tempfile
import uuid

ROOT = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "scripts"))
import mock_supabase_server as mss  # noqa: E402
import web_ui_e2e as e2e  # noqa: E402

fails = []


def window(body, key="چک‌لیست کد", n=420):
    i = body.find(key)
    return body[max(0, i - 40):i + n] if i >= 0 else "(پیدا نشد) " + body[-200:]


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + extra) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def main():
    from playwright.sync_api import sync_playwright

    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()

    teacher = next(u for u in state.users_by_id.values() if u.get("role") == "teacher")
    student = next(u for u in state.users_by_id.values() if u.get("role") == "student")
    eid = "exam-" + uuid.uuid4().hex[:8]
    qid = "q-" + uuid.uuid4().hex[:6]
    state.exams[eid] = {
        "id": eid, "teacher_id": teacher["id"], "title": "آزمون کد V356",
        "subject": "برنامه‌نویسی", "total_score": 4.0, "neg_marking": False,
        "questions": [{"id": qid, "type": "essay", "mode": "code", "code_lang": "python",
                       "text": "دو عدد را بگیر و جمعشان را چاپ کن.", "score": 4.0}],
        "is_active": True, "created_at": state.now_iso(),
    }
    aid = "ans-" + uuid.uuid4().hex[:8]
    state.answers[aid] = {
        "id": aid, "exam_id": eid, "student_id": student["id"],
        "student_name": "دانش‌آموز کد",
        "responses": ["```python\na, b = map(int, input().split())\nprint(a + b)\n```"],
        "grades": [0.0], "total_grade": 0.0, "feedback": "",
        "part_grades": {}, "part_feedback": {}, "graded": False, "auto_graded": False,
        "response_images": {}, "attempt_no": 1, "submitted_at": state.now_iso(), "meta": {},
    }

    with tempfile.TemporaryDirectory(prefix="v356_site_") as tmp:
        site = pathlib.Path(tmp)
        e2e.build_chunked_site_out(site, anon)
        web, url = e2e.start_readonly_site_server(site, mock_url, anon)
        try:
            with sync_playwright() as pw:
                br = pw.chromium.launch()
                page = br.new_page(viewport={"width": 1440, "height": 950})
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))
                page.goto(url + "/", wait_until="domcontentloaded")
                page.locator("input[type='text']").first.fill("uitest_teacher")
                page.locator("input[type='password']").first.fill("TestPass1234!")
                page.locator("input[type='password']").first.press("Enter")
                page.locator("button[aria-label='افزودن']").wait_for()

                print("\n=== تعریف موارد آزمون ===")
                page.evaluate(f"() => window.ExamSite.go('grading', {{examId: '{eid}', tab: 'grading'}})")
                page.get_by_text("پاسخ در انتظار تصحیح", exact=False).first.wait_for(timeout=15000)
                e2e.click_visible_button(page, "موارد آزمون کد")
                page.wait_for_timeout(500)
                areas = page.locator(".modal-bg textarea")
                areas.nth(0).fill("a, b = map(int, input().split())\nprint(a + b)")
                areas.nth(1).fill("مثال ساده | 2 3 => 5\nعدد منفی | 2 | -2 3 => 1\nحالت مرزی | پنهان | 0 0 => 0")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(900)
                ok("سه مورد آزمون ذخیره شد", len(state.code_tests) == 3, str(len(state.code_tests)))
                ok("یک مورد پنهان ثبت شد", sum(1 for x in state.code_tests if x["hidden"]) == 1)
                ok("پاسخ مرجع ذخیره شد", any("print(a + b)" in s["reference"] for s in state.code_specs.values()))
                ok("وزن مورد دوم دو است", any(x["weight"] == 2 for x in state.code_tests))
                page.locator(".modal-bg .x").first.click()
                page.wait_for_timeout(400)

                print("\n=== چک‌لیست در تب تصحیح ===")
                page.locator(".g-item").first.click()
                page.wait_for_timeout(700)
                body = page.evaluate("() => document.body.innerText")
                ok("چک‌لیست ظاهر شد", "چک‌لیست کد" in body, body[-500:])
                ok("هشدار «کد اجرا نمی‌شود» دیده می‌شود", "کد اجرا نمی‌شود" in body)
                ok("وضعیت «هنوز بررسی نشده» است", "هنوز بررسی نشده" in body)
                ok("مورد پنهان نشان داده شد", "پنهان" in body)
                ok("پیشنهاد صفر است", "پیشنهاد: ۰" in body, window(body))

                print("\n=== علامت‌زدن و پیشنهاد نمره ===")
                boxes = page.locator(".card input[type='checkbox']")
                boxes.nth(0).check()
                page.wait_for_timeout(600)
                body = page.evaluate("() => document.body.innerText")
                ok("یک مورد قبول ثبت شد", len(state.code_checks) == 1, str(len(state.code_checks)))
                # وزن ۱ از ۴ → ۱ از ۴ نمره
                ok("پیشنهاد به ۱ تغییر کرد", "پیشنهاد: ۱" in body, window(body))
                boxes.nth(1).check()
                page.wait_for_timeout(600)
                body = page.evaluate("() => document.body.innerText")
                # وزن ۱+۲ از ۴ → ۳ از ۴ نمره
                ok("پیشنهاد به ۳ تغییر کرد", "پیشنهاد: ۳" in body, window(body))
                ok("وضعیت بررسی ناقص است", "بررسی ناقص" in body)

                print("\n=== مقایسهٔ خروجی ===")
                actual = page.locator(".card input[type='text']").first
                actual.fill("5")
                actual.dispatch_event("change")
                page.wait_for_timeout(600)
                body = page.evaluate("() => document.body.innerText")
                ok("خروجی مطابق تشخیص داده شد", "✓ مطابق" in body, body[-400:])
                actual.fill("9")
                actual.dispatch_event("change")
                page.wait_for_timeout(500)
                body = page.evaluate("() => document.body.innerText")
                ok("خروجی نامطابق تشخیص داده شد", "✗ نامطابق" in body)

                print("\n=== نمره خودکار ذخیره نمی‌شود ===")
                before = state.answers[aid]["total_grade"]
                e2e.click_visible_button(page, "اعمال پیشنهاد")
                page.wait_for_timeout(500)
                ok("اعمال پیشنهاد چیزی روی سرور ذخیره نکرد",
                   state.answers[aid]["total_grade"] == before, str(state.answers[aid]["total_grade"]))
                ok("پیام «ذخیره با خود شماست» دیده شد", "ذخیره با خود شماست" in page.evaluate("() => document.body.innerText"))

                print("\n=== محرمانگی ===")
                ok("موردهای پنهان در پاسخ دانش‌آموز نیستند",
                   all("hidden" not in str(r) for r in state.answers[aid]["responses"]))
                ok("خطای جاوااسکریپت رخ نداد", not errors, str(errors[:2]))
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    print(f"\nV356_BROWSER: {'PASS' if not fails else 'FAIL'} ({len(fails)} خطا)")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
