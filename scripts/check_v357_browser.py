#!/usr/bin/env python3
"""V357 — بررسی زمان‌بندی و حالت آفلاین در مرورگر واقعی.

می‌سنجد: معلم زمان اضافه با دلیل ثبت می‌کند، محدودیت سؤال ذخیره می‌شود،
دانش‌آموز شمارش معکوس می‌بیند، سؤال منقضی قفل می‌شود، و بستهٔ آفلاین
گرفته و ارسال می‌شود و برگه نشان «آفلاین» می‌گیرد.
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


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:300]) if extra and not cond else ""))
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
    state.exams[eid] = {
        "id": eid, "teacher_id": teacher["id"], "title": "آزمون زمان‌بندی V357",
        "subject": "ریاضی", "total_score": 6.0, "neg_marking": False, "duration": 60,
        "questions": [
            {"id": "q1", "type": "essay", "text": "سؤال یک", "score": 2.0},
            {"id": "q2", "type": "essay", "text": "سؤال دو", "score": 2.0},
            {"id": "q3", "type": "essay", "text": "سؤال سه", "score": 2.0},
        ],
        "is_active": True, "created_at": state.now_iso(),
    }
    aid = "ans-" + uuid.uuid4().hex[:8]
    state.answers[aid] = {
        "id": aid, "exam_id": eid, "student_id": student["id"], "student_name": "دانش‌آموز زمان",
        "responses": ["", "", ""], "grades": [0.0, 0.0, 0.0], "total_grade": 0.0,
        "feedback": "", "part_grades": {}, "part_feedback": {}, "graded": False,
        "auto_graded": False, "response_images": {}, "attempt_no": 1,
        "submitted_at": state.now_iso(), "meta": {},
    }

    with tempfile.TemporaryDirectory(prefix="v357_site_") as tmp:
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

                print("\n=== تب زمان‌بندی ===")
                page.evaluate(f"() => window.ExamSite.go('grading', {{examId: '{eid}', tab: 'timing'}})")
                page.get_by_text("محدودیت زمان سؤال‌ها", exact=False).first.wait_for(timeout=15000)
                body = page.evaluate("() => document.body.innerText")
                ok("تب زمان‌بندی باز شد", "زمان اضافهٔ فردی" in body, body[-300:])
                ok("هشدار قفل سمت سرور دیده می‌شود", "قفل سمت سرور اعمال می‌شود" in body)

                print("\n=== محدودیت زمان سؤال ===")
                e2e.click_visible_button(page, "ویرایش")
                page.wait_for_timeout(500)
                ta = page.locator(".modal-bg textarea").first
                ta.fill("همه = ۳۰۰\n۱ = ")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(600)
                ok("رقم فارسی پذیرفته و خطا درست نشان داده شد",
                   "ثانیه باید عدد صحیح باشد" in page.evaluate("() => document.body.innerText"))
                ta.fill("همه = ۳۰۰\n۱ = ۶۰")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(800)
                ok("دو قاعده ذخیره شد", len(state.question_limits.get(eid, {})) == 2,
                   page.evaluate("() => { const m=document.querySelector('.modal-bg'); return m ? m.innerText.slice(-220) : '(بسته شد)'; }"))
                ok("پیش‌فرض ۳۰۰ ثانیه ثبت شد", state.question_limits.get(eid, {}).get("-1") == 300)
                ok("سؤال ۱ شصت ثانیه ثبت شد", state.question_limits.get(eid, {}).get("0") == 60)
                # مودال پس از ذخیرهٔ موفق خودش بسته می‌شود
                if page.locator(".modal-bg").count():
                    page.locator(".modal-bg .x").first.click()
                    page.wait_for_timeout(400)
                ok("شمار قواعد در تب دیده می‌شود", "۲ قاعده" in page.evaluate("() => document.body.innerText"))

                print("\n=== زمان اضافه با دلیل اجباری ===")
                e2e.click_visible_button(page, "افزودن")
                page.wait_for_timeout(500)
                page.locator(".modal-bg textarea").first.fill("کم")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(500)
                ok("دلیل کوتاه رد شد", "دلیل زمان اضافه باید بین" in page.evaluate("() => document.body.innerText"))
                ok("چیزی روی سرور ذخیره نشد", not state.time_grants)
                page.locator(".modal-bg textarea").first.fill("مشکل یادگیری تأییدشدهٔ مشاور")
                page.locator(".modal-bg input[type='text']").first.fill("۱۵")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(800)
                ok("مجوز ۱۵ دقیقه ثبت شد", len(state.time_grants) == 1, str(state.time_grants))
                g = next(iter(state.time_grants.values()), {})
                ok("رقم فارسی ۱۵ درست خوانده شد", g.get("extra_minutes") == 15, str(g))
                ok("دلیل ذخیره شد", "مشکل یادگیری" in str(g.get("reason", "")))
                ok("زمان اضافه در جدول دیده می‌شود", "۱۵ دقیقه" in page.evaluate("() => document.body.innerText"))

                print("\n=== لغو مجوز ===")
                # دو دکمهٔ «ویرایش» هست؛ این یکی در سطر دانش‌آموز است
                page.locator("table.tbl button.btn").last.click()
                page.wait_for_timeout(500)
                page.locator(".modal-bg input[type='text']").first.fill("0")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(700)
                g = next(iter(state.time_grants.values()), {})
                ok("مجوز به صفر رسید", g.get("extra_minutes") == 0, str(g))

                print("\n=== سمت دانش‌آموز: شمارش معکوس ===")
                page2 = br.new_page(viewport={"width": 1440, "height": 950})
                page2.on("pageerror", lambda e: errors.append(str(e)))
                page2.goto(url + "/", wait_until="domcontentloaded")
                page2.locator("input[type='text']").first.fill("uitest_student")
                page2.locator("input[type='password']").first.fill("TestPass1234!")
                page2.locator("input[type='password']").first.press("Enter")
                page2.wait_for_timeout(2500)
                # سیم‌کشی سمت دانش‌آموز در همان بستهٔ بارگذاری‌شده سنجیده
                # می‌شود؛ گرفتن برگه به لوله‌کشی موجودِ آزمون بند است که
                # سرور آزمایشی کامل پیاده نکرده، پس این‌جا آزموده نمی‌شود.
                wired = page2.evaluate("""() => {
                    const src = [...document.scripts].map(s => s.src).join(' ');
                    return { timing: !!window.ExamTiming, hasPanel: src.includes('panels') || src.includes('p0.') };
                }""")
                ok("موتور زمان‌بندی و بستهٔ پنل در صفحهٔ دانش‌آموز حاضرند",
                   wired.get("timing") and wired.get("hasPanel"), str(wired))
                page2.evaluate(f"() => window.ExamSite.go('student')")
                page2.wait_for_timeout(1500)
                has_timing = page2.evaluate("() => !!window.ExamTiming")
                ok("موتور زمان‌بندی در سایت بارگذاری شد", has_timing)
                clock_ok = page2.evaluate("""() => {
                    const T = window.ExamTiming;
                    const s = T.questionState({limited:true}, Date.now()+65000, Date.now(), false);
                    return s.clock === '1:05' && !s.locked && !s.warn;
                }""")
                ok("شمارش معکوس درست حساب می‌شود", clock_ok)
                lock_ok = page2.evaluate("""() => {
                    const T = window.ExamTiming;
                    return T.questionState({limited:true}, Date.now()-1000, Date.now(), false).locked === true;
                }""")
                ok("سؤال منقضی قفل حساب می‌شود", lock_ok)

                print("\n=== بستهٔ آفلاین ===")
                pack = page2.evaluate("""async (eid) => {
                    try { return await window.ExamSite.rpcObj('native_offline_pack_v357', {p_exam: eid}); }
                    catch (e) { return {error: String(e && e.message || e)}; }
                }""", eid)
                ok("بستهٔ آفلاین صادر شد", bool(pack and pack.get("token")), str(pack)[:160])
                if pack and pack.get("token"):
                    ok("مهلت در بسته هست", bool(pack.get("expires_at")))
                    ok("محدودیت‌ها در بسته هست", pack.get("limits", {}).get("0") == 60)
                    sub = page2.evaluate("""async (a) => {
                        try { return await window.ExamSite.rpcObj('native_offline_submit_v357', {
                            p_exam: a.eid, p_token: a.token, p_responses: ['پاسخ آفلاین','',''],
                            p_events: [{t:'focus', at: Date.now(), d:'خروج'}],
                            p_finished_at: new Date().toISOString()}); }
                        catch (e) { return {error: String(e && e.message || e)}; }
                    }""", {"eid": eid, "token": pack["token"]})
                    ok("ارسال آفلاین پذیرفته شد", bool(sub and sub.get("ok")), str(sub)[:160])
                    ok("برگه نشان آفلاین گرفت", state.answers[aid].get("meta", {}).get("offline") is True)
                    ok("پاسخ آفلاین ذخیره شد",
                       state.answers[aid]["responses"][0] == "پاسخ آفلاین",
                       str(state.answers[aid]["responses"]))
                    ok("رویداد آفلاین ثبت شد",
                       len(state.answers[aid].get("meta", {}).get("offline_events", [])) == 1)
                    again = page2.evaluate("""async (a) => {
                        try { return await window.ExamSite.rpcObj('native_offline_submit_v357', {
                            p_exam: a.eid, p_token: a.token, p_responses: ['دوباره'],
                            p_events: [], p_finished_at: new Date().toISOString()}); }
                        catch (e) { return {error: String(e && e.message || e)}; }
                    }""", {"eid": eid, "token": pack["token"]})
                    ok("توکن یک‌بارمصرف است", bool(again and again.get("error")), str(again)[:160])
                ok("نمره دست‌نخورده ماند", state.answers[aid]["total_grade"] == 0.0)
                ok("خطای جاوااسکریپت رخ نداد", not errors, str(errors[:2]))
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    print(f"\nV357_BROWSER: {'PASS' if not fails else 'FAIL'} ({len(fails)} خطا)")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
