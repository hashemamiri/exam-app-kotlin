#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V352.1 — نگهبانِ سنجهٔ «صفحه خالی نیست» در آزمون رابط وب.

چرا لازم شد: قاعدهٔ V179 هنگام باز بودن موتور چاپ/کارنامه، پوستهٔ زیرین را
visibility:hidden می‌کند و innerText متن پنهان را نمی‌شمارد. سنجهٔ قدیمی
تنها به این دلیل پاس می‌شد که یک پیام شناور اتفاقاً روی صفحه بود؛ در CI آن
پیام محو شده بود و صفحهٔ کاملاً سالم «خالی» گزارش شد (گام ۰۹ب).

این نگهبان هر پنج حالت را می‌سازد و می‌سنجد که سنجه هم صفحهٔ سالم را قبول
کند و هم صفحهٔ واقعاً خالی را رد کند.

اجرا:  python3 scripts/check_layout_health.py
"""
import pathlib
import sys

SCRIPTS = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import web_ui_e2e as e2e                          # noqa: E402
from playwright.sync_api import sync_playwright   # noqa: E402

CSS = "<style>body.engine-open > #root{visibility:hidden;pointer-events:none}</style>"
LONG = "قالب کارنامه با متن کافی برای شمارش نویسه‌ها"


def page_html(root_html: str, overlay_html: str, engine: bool) -> str:
    cls = " class='engine-open'" if engine else ""
    return (
        "<html dir='rtl' lang='fa'><head><meta charset='utf-8'>" + CSS + "</head>"
        "<body" + cls + "><div id='root'>" + root_html + "</div>"
        + overlay_html + "</body></html>"
    )


def overlay(wait_text: str, frame_body: str) -> str:
    wait = ("<div class='engine-wait-txt'>" + wait_text + "</div>") if wait_text else ""
    frame = ""
    if frame_body is not None:
        doc = "<html dir='rtl' lang='fa'><body>" + frame_body + "</body></html>"
        frame = "<iframe title='report-card-templates' srcdoc=\"" + doc.replace('"', '&quot;') + "\"></iframe>"
    return "<div class='engine-bg'>" + wait + frame + "</div>"


CASES = [
    ("صفحهٔ عادی پرمتن", page_html("<p>" + LONG + "</p>", "", False), True),
    ("صفحهٔ عادی خالی", page_html("", "", False), False),
    # همان حالتی که در CI شکست: موتور باز، کارت‌ها آماده، بی پیام شناور
    ("موتور باز با کارت‌های آماده",
     page_html("<p>پوستهٔ برنامه</p>",
               overlay("", "<div class='rc-card'>" + LONG + "</div><div class='rc-card'>" + LONG + "</div>"),
               True), True),
    ("موتور باز و واقعاً خالی", page_html("<p>پوستهٔ برنامه</p>", overlay("", ""), True), False),
    ("موتور باز در حال آماده‌سازی",
     page_html("<p>پوسته</p>", overlay("در حال آماده‌سازی قالب‌های کارنامه…", ""), True), True),
]


def main() -> int:
    fails = []
    with sync_playwright() as pw:
        browser = pw.chromium.launch()
        for name, html, should_pass in CASES:
            page = browser.new_page(viewport={"width": 1366, "height": 900})
            page.set_content(html, wait_until="load")
            try:
                metrics = e2e.assert_layout_healthy(page, name)
                accepted, why = True, str(metrics["bodyTextLen"])
            except AssertionError as exc:
                accepted, why = False, str(exc)[:60]
            page.close()
            ok = accepted == should_pass
            print(("  OK   " if ok else "  FAIL ") + name
                  + " → " + ("قبول" if accepted else "رد") + " (" + why + ")")
            if not ok:
                fails.append(name)
        browser.close()
    if fails:
        print("LAYOUT_HEALTH: FAIL " + str(fails))
        return 1
    print("LAYOUT_HEALTH: PASS — %d حالت" % len(CASES))
    return 0


if __name__ == "__main__":
    sys.exit(main())
