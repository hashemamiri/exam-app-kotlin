#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.39 — نگهبان محلیِ «سایت منتشرشده» و «دامنهٔ onlineexam.ir».

بدون شبکه اجرا می‌شود (مناسب preflight و CI) و شش چیز را تضمین می‌کند:
  1) `site/pay-return.html` و `site/pwa/pay-return.html` از نظر محتوا یکسان‌اند
     (فقط خط توضیح مجاز است) — قانون V335.32d.
  2) `site.yml` نسخهٔ ریشهٔ صفحهٔ بازگشت را در `site_out/` می‌گذارد؛ نبودِ این خط
     باعث می‌شد `/pay-return.html` روی دامنه ۴۰۴ یا صفحهٔ فرود شود (یافتهٔ V335.39).
  3) `site.yml` گام هم‌گام‌سازی آروان را دارد و با متغیر `ARVAN_SYNC` قفل شده است
     (بدون سکرت، CI نباید بشکند). اگر توکن گیت مجوز `workflow` نداشته باشد این
     گام در `docs/ci/site_arvan_sync_step.yml` می‌ماند و اینجا با ⚠ گزارش می‌شود
     (خطا نیست، ولی باید یک‌بار دستی در site.yml جای‌گذاری شود).
  4) `scripts/sync_arvan_site.py` سالم کامپایل می‌شود و هیچ کلید/رمز درون‌خطی ندارد.
  5) نمونهٔ Caddy برای دارایی‌های ناموجود ۴۰۴ می‌دهد (نه index با کد ۲۰۰).
  6) پیش‌فرض مقصد هم‌گام‌سازی، همین دامنه و سرور آروان است.

اجرا:  python3 scripts/check_site_sync.py            (خروجی غیرصفر = ایراد)
"""
import os
import py_compile
import re
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ok = True


def read(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as f:
        return f.read()


def step(name, cond, detail=""):
    global ok
    print(("✅ " if cond else "❌ ") + name + ((" | " + detail) if detail and not cond else ""))
    if not cond:
        ok = False
    return cond


def step_pending(name, in_workflow, in_snippet, hint):
    """مانند step ولی «آماده ولی جای‌گذاری‌نشده» را خطا نمی‌گیرد — فقط پرصدا هشدار می‌دهد."""
    global ok
    if in_workflow:
        print("✅ " + name)
        return True
    if in_snippet:
        print("⚠️  " + name + " — در site.yml نیست ولی در docs/ci آماده است")
        print("    ↳ " + hint)
        return False
    ok = False
    print("❌ " + name + " | نه در site.yml هست و نه در docs/ci/site_arvan_sync_step.yml")
    return False


def strip_comments(html):
    return [ln.rstrip() for ln in html.splitlines() if not ln.strip().startswith("<!--")]


def main():
    root_page = read("site/pay-return.html")
    pwa_page = read("site/pwa/pay-return.html")
    step("صفحهٔ بازگشت: نسخهٔ ریشه و pwa هم‌محتوا هستند",
         strip_comments(root_page) == strip_comments(pwa_page),
         "تفاوت غیر از خط توضیح وجود دارد")
    wf = read(".github/workflows/site.yml")
    snip = read("docs/ci/site_arvan_sync_step.yml")
    pending = []
    if not step_pending("site.yml نسخهٔ ریشهٔ pay-return را منتشر می‌کند",
                        "cp site/pay-return.html site_out/" in wf,
                        "cp site/pay-return.html site_out/" in snip,
                        "خط «cp site/pay-return.html site_out/» را در گام Build اضافه کنید"):
        pending.append("cp pay-return")
    if not step_pending("site.yml گام هم‌گام‌سازی آروان را دارد",
                        "SYNC_ARVAN" in wf and "scripts/sync_arvan_site.py" in wf,
                        "SYNC_ARVAN" in snip and "scripts/sync_arvan_site.py" in snip,
                        "گام Sync domain را از docs/ci/site_arvan_sync_step.yml به site.yml ببرید"):
        pending.append("گام SYNC_ARVAN")
    step_pending("گام هم‌گام‌سازی با ARVAN_SYNC قفل است",
                 "vars.ARVAN_SYNC" in wf, "vars.ARVAN_SYNC" in snip,
                 "شرط vars.ARVAN_SYNC == 'on' لازم است")
    step_pending("گام هم‌گام‌سازی فقط روی main اجرا می‌شود",
                 re.search(r"vars\.ARVAN_SYNC == 'on'.{0,120}github\.ref_name == 'main'", wf, re.S) is not None,
                 re.search(r"vars\.ARVAN_SYNC == 'on'.{0,120}github\.ref_name == 'main'", snip, re.S) is not None,
                 "شرط github.ref_name == 'main' لازم است")
    # اسکریپت هم‌گام‌سازی
    sync_py = os.path.join(ROOT, "scripts", "sync_arvan_site.py")
    comp = True
    try:
        with tempfile.TemporaryDirectory() as td:
            py_compile.compile(sync_py, cfile=os.path.join(td, "s.pyc"), doraise=True)
    except Exception as e:  # noqa: BLE001
        comp = False
        print("   py_compile:", e)
    step("scripts/sync_arvan_site.py کامپایل می‌شود", comp)
    src = open(sync_py, encoding="utf-8").read() if os.path.isfile(sync_py) else ""
    step("اسکریپت هم‌گام‌سازی هیچ کلید خصوصی/رمز درون‌خطی ندارد",
         "-----BEGIN" not in src and "password=" not in src.lower())
    step("مقصد پیش‌فرض، سرور و دامنهٔ ماست",
         "onlineexam.ir" in src and "95.38.160.168" in src)
    caddy_rel = "relay/Caddyfile.site.example"
    caddy = read(caddy_rel) if os.path.isfile(os.path.join(ROOT, caddy_rel)) else ""
    step("نمونهٔ Caddy برای فایل ناموجود ۴۰۴ می‌دهد",
         "file_server" in caddy and ("404" in caddy or "try_files" in caddy),
         caddy_rel + " ناقص است")
    print("—" * 46)
    if pending:
        print("⚠️  کارِ باقی‌مانده: " + " · ".join(pending))
        print("   توکن گیت شما احتمالاً scope=workflow ندارد؛ قطعهٔ آماده در docs/ci/site_arvan_sync_step.yml است.")
        print("   تا جای‌گذاری دستی، هم‌گام‌سازی خودکار اجرا نمی‌شود (اسکریپت دستی کار می‌کند).")
    print(("✅ نگهبان سایت/دامنه سالم است" if ok else "❌ نگهبان سایت/دامنه ایراد دارد"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
