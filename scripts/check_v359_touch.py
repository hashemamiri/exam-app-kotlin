#!/usr/bin/env python3
"""V359 — بازتولید و سنجش نقص دکمهٔ حذف ✕ در موبایل سایت.

پیشینه: در بررسی پیشین دیده شد که `.tap()` (لمس واقعی) شمار تراشه را
۱ نگه می‌داشت ولی `element.click()` حذف می‌کرد. آن بررسی فقط تشخیص
بود و اصلاحی انجام نشد.

این اسکریپت نقص را در مرورگر با has_touch=True بازتولید می‌کند و
پس از اصلاح می‌سنجد که لمس هم کار می‌کند. هیچ حدسی دربارهٔ علت
جایگزین سنجش واقعی نمی‌شود.
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


def main():
    from playwright.sync_api import sync_playwright

    srv, state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{srv.server_address[1]}"
    anon = mss.make_public_anon_key()

    with tempfile.TemporaryDirectory(prefix="v359_touch_") as tmp:
        site = pathlib.Path(tmp)
        e2e.build_chunked_site_out(site, anon)
        web, url = e2e.start_readonly_site_server(site, mock_url, anon)
        try:
            with sync_playwright() as pw:
                br = pw.chromium.launch()
                # دستگاه لمسی واقعی: بدون has_touch، tap خطا می‌دهد و
                # نقص هرگز بازتولید نمی‌شد.
                ctx = br.new_context(
                    viewport={"width": 412, "height": 915},
                    has_touch=True, is_mobile=True,
                    user_agent="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36",
                )
                page = ctx.new_page()
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))
                page.set_default_timeout(15000)
                page.goto(url + "/", wait_until="domcontentloaded")
                # نمای موبایل نخست دکمهٔ «ورود / ثبت‌نام» دارد؛ فرم ورود
                # پشت آن است. بدون این گام، هارنس روی input منتظر می‌ماند.
                auth = page.get_by_role("button", name="ورود / ثبت‌نام")
                auth.wait_for()
                auth.click()
                page.locator("input[type='password']").first.wait_for()
                # نوع ورودی شناسه در نمای موبایل ممکن است text نباشد؛
                # هر ورودیِ غیرِ رمز را می‌گیریم تا هارنس شکننده نماند.
                ident = page.locator("input:not([type='password']):not([type='hidden']):not([type='checkbox'])").first
                ident.fill("uitest_teacher")
                page.locator("input[type='password']").first.fill("TestPass1234!")
                page.locator("input[type='password']").first.press("Enter")
                page.wait_for_timeout(2000)

                print("\n=== تشخیص: کدام دکمه‌های ✕ در دسترس لمس هستند ===")
                # هر دکمهٔ حذف را با لمس می‌آزماییم و اندازه/پوشش را می‌سنجیم.
                page.evaluate("() => window.ExamSite.go('students')")
                page.wait_for_timeout(900)

                probe = page.evaluate("""() => {
                  const out = [];
                  document.querySelectorAll('button').forEach(b => {
                    const t = (b.textContent || '').trim();
                    if (t !== '✕' && !b.getAttribute('aria-label')?.includes('حذف')) return;
                    const r = b.getBoundingClientRect();
                    const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
                    const hit = document.elementFromPoint(cx, cy);
                    out.push({
                      text: t, w: Math.round(r.width), h: Math.round(r.height),
                      covered: !!hit && hit !== b && !b.contains(hit),
                      hitTag: hit ? hit.tagName + '.' + (hit.className || '') : null,
                      touchAction: getComputedStyle(b).touchAction
                    });
                  });
                  return out;
                }""")
                print(f"  {len(probe)} دکمهٔ ✕ پیدا شد")
                for p in probe[:6]:
                    print("   ", p)

                small = [p for p in probe if p["w"] < 40 or p["h"] < 40]
                covered = [p for p in probe if p["covered"]]
                ok("هیچ دکمهٔ حذف زیر اندازهٔ لمس نیست (۴۰px)", not small,
                   f"{len(small)} مورد کوچک‌تر: {small[:3]}")
                ok("هیچ دکمهٔ حذف زیر عنصر دیگر نیست", not covered, str(covered[:3]))

                print("\n=== لمس واقعی روی تراشهٔ حذف‌شدنی ===")
                # پنجرهٔ فیلتر دانش‌آموز: تراشه‌ها و دکمهٔ ✕
                touched = page.evaluate("""() => {
                  const btns = [...document.querySelectorAll('button')];
                  const f = btns.find(b => (b.getAttribute('aria-label')||'').includes('فیلتر'));
                  if (f) { f.click(); return 'filter'; }
                  return null;
                }""")
                page.wait_for_timeout(600)
                if touched:
                    # یک تراشهٔ «انتخاب‌شدنی» را با لمس فعال کن. تراشهٔ
                    # «همه پایه‌ها» مقدار را پاک می‌کند و هرگز .on نمی‌شود؛
                    # اگر آن را می‌گرفتیم، سنجهٔ «۰ پس از حذف» تُهی و
                    # به‌دروغ سبز می‌شد.
                    # بخش‌ها آکاردئونی‌اند: تا «پایه» باز نشود تراشه‌ای
                    # در DOM نیست. هارنس باید مسیر واقعی کاربر را برود.
                    page.locator(".m-fsec-h").first.tap()
                    page.wait_for_timeout(400)
                    picked = page.evaluate("""() => {
                      const cs = [...document.querySelectorAll('.m-fchip')];
                      const c = cs.find(x => !(x.textContent||'').includes('همه'));
                      if (!c) return null;
                      c.click();
                      return (c.textContent || '').trim();
                    }""")
                    page.wait_for_timeout(500)
                    chips_on = page.evaluate("() => document.querySelectorAll('.m-fchip.on').length")
                    ok("تراشه با کلیک واقعاً انتخاب می‌شود", chips_on > 0,
                       f"تراشهٔ «{picked}» → شمار فعال={chips_on}")
                    chips_before = chips_on
                    clear = page.locator("button.m-textbtn", has_text="حذف فیلترها").first
                    if clear.count():
                        clear.tap()
                        page.wait_for_timeout(600)
                        # نکتهٔ مهم: تراشهٔ «همه پایه‌ها» وقتی هیچ فیلتری
                        # نیست خودش .on می‌شود. پس «شمار .on == ۰» سنجهٔ
                        # غلطی است و پیش‌تر به‌اشتباه نقص گزارش شده بود.
                        # سنجهٔ درست: نام تراشهٔ انتخاب‌شده باید برود و
                        # «همه» جایش بیاید.
                        after = page.evaluate(
                            "() => [...document.querySelectorAll('.m-fchip.on')].map(x => x.textContent.trim())"
                        )
                        ok("لمس «حذف فیلترها» انتخاب را پاک می‌کند",
                           picked not in after and any("همه" in a for a in after),
                           f"پیش={picked!r} پس={after}")
                    else:
                        ok("دکمهٔ حذف فیلتر پیدا شد", False, "یافت نشد")
                    # اندازهٔ هدفِ لمس داخل پنجرهٔ باز سنجیده می‌شود.
                    inner = page.evaluate("""() => {
                      const out = [];
                      document.querySelectorAll('.m-fbar button, .m-fchip').forEach(b => {
                        const r = b.getBoundingClientRect();
                        const hit = document.elementFromPoint(r.left + r.width/2, r.top + r.height/2);
                        out.push({t:(b.textContent||'').trim().slice(0,14),
                                  w:Math.round(r.width), h:Math.round(r.height),
                                  covered: !!hit && hit !== b && !b.contains(hit)});
                      });
                      return out;
                    }""")
                    tiny = [i for i in inner if i["h"] < 40]
                    hidden = [i for i in inner if i["covered"]]
                    print(f"  {len(inner)} هدف لمس داخل پنجره؛ کوچک‌تر از ۴۰px: {len(tiny)}")
                    for i in inner[:5]:
                        print("   ", i)
                    ok("هدف‌های لمس پنجره پوشیده نیستند", not hidden, str(hidden[:3]))
                    x = page.locator(".m-fbar .m-iconbtn").first
                    if x.count():
                        x.tap()
                        page.wait_for_timeout(500)
                        gone = page.evaluate("() => !document.querySelector('.m-fbar')")
                        ok("لمس ✕ پنجره را می‌بندد", gone)
                    else:
                        ok("دکمهٔ ✕ پیدا شد", False, "یافت نشد")
                else:
                    ok("دکمهٔ فیلتر موبایل پیدا شد", False, touched)

                ok("خطای جاوااسکریپت نیست", not errors, str(errors[:2]))
                ctx.close()
                br.close()
        finally:
            web.shutdown()
            srv.shutdown()

    print()
    if fails:
        print(f"V359_TOUCH: FAIL ({len(fails)} مورد)")
        for f in fails:
            print("  · " + f)
        return 1
    print("V359_TOUCH: PASS (0 خطا)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
