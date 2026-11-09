#!/usr/bin/env python3
"""V358 — بانک سؤال در مرورگر واقعی: درختواره، پالایش دشواری، انتخاب خودکار.

می‌سنجد: مسیر کامل دسته در پالایشگر دیده می‌شود، انتخاب یک فصل
زیرمجموعه‌هایش را هم می‌گیرد، پالایش دشواری درست کار می‌کند، دستهٔ
فرزند ساخته می‌شود، و نقشهٔ آزمون سؤال برمی‌دارد و به آزمون می‌افزاید.
"""
import pathlib
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "scripts"))
import mock_supabase_server as mss  # noqa: E402
import web_ui_e2e as e2e  # noqa: E402

fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:300]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def fa(n):
    return "".join(chr(0x6F0 + int(d)) for d in str(n))


def main():
    from playwright.sync_api import sync_playwright

    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()

    # درخت: ریاضی <- فصل ۱ <- معادله ؛ ریاضی <- فصل ۲
    state.bank_categories = [
        {"id": 1, "name": "ریاضی", "parent_id": None},
        {"id": 2, "name": "فصل " + fa(1), "parent_id": 1},
        {"id": 3, "name": "معادله", "parent_id": 2},
        {"id": 4, "name": "فصل " + fa(2), "parent_id": 1},
        {"id": 5, "name": "فیزیک", "parent_id": None},
    ]
    state.bank_items = []
    for i in range(1, 13):
        state.bank_items.append({
            "id": i,
            "question": {"id": f"q{i}", "type": "essay", "text": f"پرسش {fa(i)}", "score": 1.0,
                         "difficulty": (i % 3) + 1, "topic": "معادله" if i % 2 else "نور"},
            "subject": "ریاضی" if i <= 8 else "فیزیک",
            "cat_ids": [3] if i <= 6 else ([4] if i <= 8 else [5]),
            "shared": False,
            "created_at": "2026-10-23T12:00:00Z",
        })

    with tempfile.TemporaryDirectory(prefix="v358_site_") as tmp:
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

                print("\n=== صفحهٔ بانک: درختواره ===")
                page.evaluate("() => window.ExamSite.go('bank')")
                page.get_by_text("سؤال جدید در بانک", exact=False).first.wait_for(timeout=15000)
                page.wait_for_timeout(600)
                opts = page.evaluate("""() => [...document.querySelectorAll('select')].map(s => [...s.options].map(o => o.text))""")
                flat = [x for grp in opts for x in grp]
                ok("پالایشگر دسته ساخته شد", any("همهٔ دسته‌ها" in x for x in flat), str(opts)[:200])
                ok("مسیر تورفتگی دارد", any("—" in x and "معادله" in x for x in flat), str(flat)[:300])
                ok("پالایشگر دشواری ساخته شد", any("همهٔ سطح‌ها" in x for x in flat))
                ok("سه سطح دشواری حاضر است",
                   all(any(lb == x for x in flat) for lb in ["ساده", "متوسط", "دشوار"]), str(flat)[:300])
                ok("سطح چهارم ساخته نشد", not any("المپیادی" in x for x in flat))

                print("\n=== پالایش زیردرخت ===")
                before = page.evaluate("() => document.querySelectorAll('.bank-card').length")
                ok("همهٔ سؤال‌ها دیده می‌شوند", before == 12, before)
                # انتخاب «ریاضی» باید ۸ سؤال بدهد (فصل ۱ و ۲ زیرش)
                page.evaluate("""() => {
                  const s = [...document.querySelectorAll('select')].find(x => [...x.options].some(o => o.text.includes('همهٔ دسته‌ها')));
                  const o = [...s.options].find(o => o.text.trim().startsWith('ریاضی'));
                  s.value = o.value; s.dispatchEvent(new Event('change'));
                }""")
                page.wait_for_timeout(400)
                sub = page.evaluate("() => document.querySelectorAll('.bank-card').length")
                ok("انتخاب درس، زیرمجموعه‌ها را هم می‌گیرد", sub == 8, sub)

                print("\n=== پالایش دشواری ===")
                page.evaluate("""() => {
                  const s = [...document.querySelectorAll('select')].find(x => [...x.options].some(o => o.text.includes('همهٔ سطح‌ها')));
                  const o = [...s.options].find(o => o.text === 'ساده');
                  s.value = o.value; s.dispatchEvent(new Event('change'));
                }""")
                page.wait_for_timeout(400)
                easy = page.evaluate("() => document.querySelectorAll('.bank-card').length")
                want = sum(1 for it in state.bank_items if it["subject"] == "ریاضی" and it["question"]["difficulty"] == 1)
                ok("پالایش دشواری درست شمارش می‌کند", easy == want, f"{easy} != {want}")
                body = page.evaluate("() => document.body.innerText")
                ok("برچسب دشواری روی کارت دیده می‌شود", "ساده" in body)
                ok("برچسب مبحث روی کارت دیده می‌شود", "معادله" in body)

                print("\n=== ساخت دستهٔ فرزند ===")
                e2e.click_visible_button(page, "دسته‌ها")
                page.wait_for_timeout(600)
                page.locator(".modal-bg input").first.fill("هندسه")
                page.evaluate("""() => {
                  const s = [...document.querySelectorAll('.modal-bg select')].pop();
                  const o = [...s.options].find(o => o.text.includes('فصل') && o.text.includes('ریاضی'));
                  s.value = o.value; s.dispatchEvent(new Event('change'));
                }""")
                page.locator(".modal-bg button.btn").last.click()
                page.wait_for_timeout(800)
                newcat = next((c for c in state.bank_categories if c["name"] == "هندسه"), None)
                ok("دستهٔ فرزند ذخیره شد", newcat is not None, str(state.bank_categories)[-200:])
                ok("والد درست ثبت شد", newcat and newcat.get("parent_id") in (2, 4), newcat)

                print("\n=== انتخاب خودکار در سازنده ===")
                page.evaluate("() => window.ExamSite.go('builder', {mode: 'online'})")
                page.wait_for_timeout(1200)
                page.evaluate("""() => {
                  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('بانک سؤال'));
                  if (b) b.click();
                }""")
                page.wait_for_timeout(900)
                ok("پنجرهٔ بانک باز شد", "بانک سؤال" in page.evaluate("() => document.body.innerText"))
                page.evaluate("""() => {
                  const b = [...document.querySelectorAll('.modal-bg button')].find(x => x.textContent.includes('انتخاب خودکار'));
                  if (b) b.click();
                }""")
                page.wait_for_timeout(600)
                ok("پنجرهٔ نقشه آزمون باز شد",
                   "انتخاب خودکار از بانک" in page.evaluate("() => document.body.innerText"))
                ta = page.locator(".modal-bg textarea").last
                ta.fill("همه | هر | " + fa(100))
                page.wait_for_timeout(500)
                txt = page.evaluate("() => document.body.innerText")
                ok("کمبود گزارش می‌شود", "کمبود" in txt, txt[-250:])
                ta.fill("معادله | ساده | " + fa(2))
                page.wait_for_timeout(500)
                txt = page.evaluate("() => document.body.innerText")
                ok("جدول نتیجه ساخته شد", "خواسته" in txt and "یافت‌شده" in txt, txt[-250:])
                add = page.locator(".modal-bg button.btn").last
                label = add.inner_text()
                ok("دکمهٔ افزودن تعداد را نشان می‌دهد", fa(2) in label, label)
                seed_before = page.evaluate("""() => {
                  const t = [...document.querySelectorAll('.modal-bg table tbody tr')].map(r => r.innerText);
                  return t.join('|');
                }""")
                page.evaluate("""() => {
                  const b = [...document.querySelectorAll('.modal-bg button')].find(x => x.textContent.includes('قرعهٔ تازه'));
                  if (b) b.click();
                }""")
                page.wait_for_timeout(400)
                ok("قرعهٔ تازه پنجره را نمی‌شکند",
                   "یافت‌شده" in page.evaluate("() => document.body.innerText"), seed_before)
                add.click()
                page.wait_for_timeout(900)
                count = page.evaluate("""() => {
                  const s = window.__builderState || null;
                  return document.querySelectorAll('.q-item, .b-q-item, .qrow').length;
                }""")
                ok("سؤال‌ها به آزمون افزوده شدند", count >= 2 or "افزوده شد" in page.evaluate("() => document.body.innerText"),
                   f"count={count}")

                ok("هیچ خطای جاوااسکریپتی رخ نداد", not errors, "; ".join(errors)[:300])
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    print(f"\nV358_BROWSER: {'PASS' if not fails else 'FAIL'} ({len(fails)} خطا)")
    return 1 if fails else 0


if __name__ == "__main__":
    raise SystemExit(main())
