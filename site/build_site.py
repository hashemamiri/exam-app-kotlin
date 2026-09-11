#!/usr/bin/env python3
"""V138 — سازندهٔ سایت تک‌فایلی: site/index.html

همهٔ منابع (CSS/JS سایت، موتور پیش‌نمایش/چاپ assets/print و ویرایشگر فرمول
assets/formula_editor/formula.html، قلم‌ها) در یک فایل HTML تعبیه می‌شوند تا
سایت بدون سرور و بدون فایل جانبی باز شود. تصاویر اطلس (figure_atlas، ۲٫۳MB)
تعبیه نمی‌شوند و از GitHub Raw همین مخزن خوانده می‌شوند.

اجرا (از ریشهٔ مخزن):  python3 site/build_site.py
"""
import base64
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
PRINT = os.path.join(ASSETS, "print")
WEB = os.path.join(PRINT, "web")
SITE = os.path.join(ROOT, "site")
ATLAS_BASE = "https://raw.githubusercontent.com/hashemamiri/exam-app-kotlin/main/app/src/main/assets/figure_atlas/"
SUPABASE_URL = "https://eazwuyrymsvdkwckdpco.supabase.co"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def read_b64(path):
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("ascii")


def font_data_url(path):
    return "data:font/ttf;base64," + read_b64(path)


def load_order():
    """ترتیب دقیق CSS/JS از exam_print_renderer.html خوانده می‌شود (نه حدس)."""
    html = read(os.path.join(PRINT, "exam_print_renderer.html"))
    css = re.findall(r'<link rel="stylesheet" href="web/([^"]+)">', html)
    js = re.findall(r'<script src="web/([^"]+)"></script>', html)
    if not css or not js:
        sys.exit("exam_print_renderer.html: ترتیب بارگذاری پیدا نشد")
    return css, js


def js_string(s):
    """رشتهٔ JS امن برای قرارگرفتن داخل <script> صفحهٔ میزبان."""
    return json.dumps(s, ensure_ascii=False).replace("</", "<\\/").replace("<!--", "<\\!--")


def build_print_engine():
    css_files, js_files = load_order()
    fonts = {
        "/fonts/shabnam_regular.ttf": os.path.join(ROOT, "app/src/main/res/font/shabnam_regular.ttf"),
        "/fonts/sahel_regular.ttf": os.path.join(ROOT, "app/src/main/res/font/sahel_regular.ttf"),
        "/fonts/bnazanin.ttf": os.path.join(ASSETS, "fonts/bnazanin.ttf"),
        "/fonts/bnazanin_bold.ttf": os.path.join(ASSETS, "fonts/bnazanin_bold.ttf"),
    }
    parts = ['<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8">',
             '<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">',
             '<title>آزمون‌ساز - پیش‌نمایش و چاپ</title>',
             # همان پرچم میزبانِ برنامه + پلِ چاپ از صفحهٔ والد (ExamPrintBridge اندروید → JS)
             '<script>window.__appHost = true; try { window.ExamPrintBridge = window.parent.__printBridge || null; } catch (e) {}</script>']
    for name in css_files:
        css = read(os.path.join(WEB, name))
        for url, path in fonts.items():
            css = css.replace('url("%s")' % url, 'url("%s")' % font_data_url(path))
        parts.append("<style>%s</style>" % css)
    parts.append("</head><body>")
    for name in js_files:
        js = read(os.path.join(WEB, name))
        js = js.replace("'/figure_atlas/", "'" + ATLAS_BASE)
        parts.append("<script>%s</script>" % js)
        if name == "host_dom.js":
            parts.append("<script>document.write(window.__APP_HOST_DOM);</script>")
    parts.append("</body></html>")
    return "".join(parts)


def build_formula_engine():
    html = read(os.path.join(ASSETS, "formula_editor", "formula.html"))
    bridge = '<script>try { window.ExamEditorNative = window.parent.__formulaBridge || null; } catch (e) {}</script>'
    marker = '<meta charset="UTF-8">'
    if marker not in html:
        sys.exit("formula.html: <meta charset> پیدا نشد")
    html = html.replace(marker, marker + bridge, 1)
    # اسکریپت باقی‌ماندهٔ Cloudflare (cdn-cgi/challenge-platform) در وب درخواست بیرونی می‌زند؛ حذف می‌شود.
    html = re.sub(r"<script>\(function\(\)\{function c\(\)\{[^\n]*?cdn-cgi/challenge-platform[^\n]*?</script>", "", html, count=1)
    if "cdn-cgi/challenge-platform" in html:
        sys.exit("formula.html: اسکریپت cdn-cgi حذف نشد")
    return html


def main():
    tpl = read(os.path.join(SITE, "src", "template.html"))
    site_css = read(os.path.join(SITE, "src", "site.css"))
    site_js = read(os.path.join(SITE, "src", "app.js")) + "\n" + read(os.path.join(SITE, "src", "builder.js")) + "\n" + read(os.path.join(SITE, "src", "student.js")) + "\n" + read(os.path.join(SITE, "src", "admin.js")) + "\n" + read(os.path.join(SITE, "src", "school.js")) + "\n" + read(os.path.join(SITE, "src", "extras.js"))
    vazir = read(os.path.join(WEB, "vazirmatn_embed.css"))
    engines = "window.__ENGINES = {print: %s, formula: %s};" % (js_string(build_print_engine()), js_string(build_formula_engine()))
    out = (tpl.replace("/*__SUPABASE_URL__*/", SUPABASE_URL)
              .replace("/*__VAZIR_CSS__*/", vazir)
              .replace("/*__SITE_CSS__*/", site_css)
              .replace("/*__ENGINES_JS__*/", engines)
              .replace("/*__SITE_JS__*/", site_js.replace("</script", "<\\/script")))
    for m in ("/*__SUPABASE_URL__*/", "/*__VAZIR_CSS__*/", "/*__SITE_CSS__*/", "/*__ENGINES_JS__*/", "/*__SITE_JS__*/"):
        if m in out:
            sys.exit("جای‌نگهدار جایگزین نشد: " + m)
    dest = os.path.join(SITE, "index.html")
    with open(dest, "w", encoding="utf-8") as f:
        f.write(out)
    print("site/index.html نوشته شد: %.1f MB" % (os.path.getsize(dest) / 1048576.0))


if __name__ == "__main__":
    main()
