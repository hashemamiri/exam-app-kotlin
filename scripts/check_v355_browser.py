#!/usr/bin/env python3
"""V355 — بررسی هدفمند تب شباهت و دستیار کلیدواژه در مرورگر واقعی.

این اسکریپت مکمل web_ui_e2e.py است: سه پاسخ تشریحی می‌کارَد که دو تای
آن‌ها یک متن با نویسه‌های فارسی/عربی متفاوت‌اند، بعد می‌سنجد که تب
شباهت آن‌ها را می‌گیرد، پاسخ نامرتبط را نمی‌گیرد، و دستیار کلیدواژه
پیشنهاد نمره می‌سازد بدون آنکه چیزی خودکار ذخیره شود.
"""
import pathlib
import sys
import tempfile
import uuid

ROOT = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "scripts"))
import mock_supabase_server as mss  # noqa: E402
import web_ui_e2e as e2e  # noqa: E402

A = "قضیه فیثاغورس در مثلث قائم الزاویه بیان می کند که مجذور وتر برابر است با مجموع مجذور دو ضلع دیگر"
B = "قضيه فيثاغورس در مثلث قائم الزاويه بيان مي كند که مجذور وتر برابر است با مجموع مجذور دو ضلع ديگر"
C = "امروز هوا بسیار خوب بود و من به پارک رفتم و با دوستانم بازی کردم و خیلی خوش گذشت"

fails = []


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
    students = [u for u in state.users_by_id.values() if u.get("role") == "student"]
    eid = "exam-" + uuid.uuid4().hex[:8]
    qid = "q-" + uuid.uuid4().hex[:6]
    state.exams[eid] = {
        "id": eid, "teacher_id": teacher["id"], "title": "آزمون شباهت V355",
        "subject": "ریاضی", "total_score": 4.0, "neg_marking": False,
        "questions": [{"id": qid, "type": "essay", "text": "قضیهٔ فیثاغورس را بیان کنید.", "score": 4.0}],
        "is_active": True, "created_at": state.now_iso(),
    }
    texts = [A, B, C]
    for i in range(3):
        aid = "ans-" + uuid.uuid4().hex[:8]
        stu = students[i % len(students)]
        state.answers[aid] = {
            "id": aid, "exam_id": eid, "student_id": stu["id"],
            "student_name": "دانش‌آموز " + "۱۲۳"[i], "responses": [texts[i]],
            "grades": [0.0], "total_grade": 0.0, "feedback": "",
            "part_grades": {}, "part_feedback": {}, "graded": False, "auto_graded": False,
            "response_images": {}, "attempt_no": 1, "submitted_at": state.now_iso(), "meta": {},
        }

    with tempfile.TemporaryDirectory(prefix="v355_site_") as tmp:
        site = pathlib.Path(tmp)
        e2e.build_chunked_site_out(site, anon)
        web, url = e2e.start_readonly_site_server(site, mock_url, anon)
        try:
            with sync_playwright() as pw:
                br = pw.chromium.launch()
                page = br.new_page(viewport={"width": 1440, "height": 900})
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))
                page.goto(url + "/", wait_until="domcontentloaded")
                page.locator("input[type='text']").first.fill("uitest_teacher")
                page.locator("input[type='password']").first.fill("TestPass1234!")
                page.locator("input[type='password']").first.press("Enter")
                page.locator("button[aria-label='افزودن']").wait_for()

                print("\n=== تب شباهت ===")
                page.evaluate(f"() => window.ExamSite.go('grading', {{examId: '{eid}', tab: 'similarity'}})")
                page.get_by_text("شباهت فقط هشدار است", exact=False).first.wait_for(timeout=15000)
                page.wait_for_timeout(700)
                body = page.evaluate("() => document.body.innerText")
                ok("تب شباهت باز شد و هشدار «نمره کم نمی‌شود» را نشان می‌دهد", "نمره‌ای را کم نمی‌کند" in body)
                ok("جفت مشکوک پیدا شد", "جفت مشکوک" in body, body[-400:])
                # ترتیب دو نام بر پایهٔ شناسهٔ پاسخ است (کلید پایدار)، نه ترتیب دانش‌آموزان
                ok("دانش‌آموز ۱ و ۲ با هم جفت شده‌اند",
                   ("دانش‌آموز ۱ ↔ دانش‌آموز ۲" in body) or ("دانش‌آموز ۲ ↔ دانش‌آموز ۱" in body), body[-400:])
                ok("پاسخ نامرتبط جفت نشده است", "دانش‌آموز ۳" not in body)
                ok("درصد شباهت ۰۰ نشان داده شد", "۱۰۰٪" in body)
                ok("خطای جاوااسکریپت رخ نداد", not errors, str(errors[:2]))

                print("\n=== مقایسهٔ دو پاسخ ===")
                e2e.click_visible_button(page, "مقایسهٔ متن")
                page.wait_for_timeout(400)
                dlg = page.evaluate("() => document.body.innerText")
                ok("پنجرهٔ مقایسه باز شد", "مقایسهٔ دو پاسخ" in dlg)
                page.locator(".modal-bg .x").first.click()
                page.wait_for_timeout(200)

                print("\n=== ثبت تصمیم معلم ===")
                e2e.click_visible_button(page, "مشکلی نبود")
                page.wait_for_timeout(300)
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(700)
                body = page.evaluate("() => document.body.innerText")
                ok("وضعیت به «مشکلی نبود» تغییر کرد", "مشکلی نبود" in body)
                ok("پیام «نمره‌ای تغییر نکرد» دیده شد", "نمره‌ای تغییر نکرد" in body)
                grades = [a["total_grade"] for a in state.answers.values() if a["exam_id"] == eid]
                ok("نمره‌ها دست‌نخورده ماندند", all(g == 0.0 for g in grades), str(grades))
                ok("تصمیم روی سرور ثبت شد", len(state.similarity_reviews) == 1)

                print("\n=== ویرایشگر کلیدواژه و دستیار ===")
                page.evaluate(f"() => window.ExamSite.go('grading', {{examId: '{eid}', tab: 'grading'}})")
                page.get_by_text("پاسخ در انتظار تصحیح", exact=False).first.wait_for(timeout=15000)
                e2e.click_visible_button(page, "کلیدواژه‌های تصحیح تشریحی")
                page.wait_for_timeout(400)
                ta = page.locator(".modal-bg textarea").first
                ta.fill("فیثاغورس = 2\nوتر\nمجذور|توان دو")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(900)
                ok("کلیدواژه‌ها روی سرور ذخیره شدند", len(state.essay_keywords) == 3,
                   str(len(state.essay_keywords)))
                page.wait_for_timeout(600)
                # پنجرهٔ کلیدواژه پس از ذخیره باز می‌ماند؛ باید بسته شود
                page.locator(".modal-bg .x").first.click()
                page.wait_for_timeout(500)
                page.locator(".g-item").first.click()
                page.wait_for_timeout(600)
                body = page.evaluate("() => document.body.innerText")
                ok("پنل دستیار در تب تصحیح دیده می‌شود", "دستیار تصحیح" in body, body[-500:])
                ok("کلیدواژهٔ آمده فهرست شد", "آمده (" in body)
                ok("دکمهٔ اعمال پیشنهاد هست", "اعمال پیشنهاد" in body)
                before = [a["total_grade"] for a in state.answers.values() if a["exam_id"] == eid]
                e2e.click_visible_button(page, "اعمال پیشنهاد")
                page.wait_for_timeout(400)
                after = [a["total_grade"] for a in state.answers.values() if a["exam_id"] == eid]
                ok("اعمال پیشنهاد چیزی روی سرور ذخیره نکرد", before == after, f"{before} → {after}")
                ok("خطای جاوااسکریپت رخ نداد (پایان)", not errors, str(errors[:2]))
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    print(f"\nV355_BROWSER: {'PASS' if not fails else 'FAIL'} ({len(fails)} خطا)")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
