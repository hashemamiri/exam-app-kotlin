#!/usr/bin/env python3
"""
V333 — ربات تست خودکار بصری و سرتاسری (Visual & E2E) وب‌اپلیکیشن روی مرورگر واقعی Chromium

سناریوهای پوشش‌داده‌شده در هر اجرای CI (پیش از انتشار روی onlineexam.ir) و اجرای محلی:
۱) بررسی بصری، راست‌به‌چپ (RTL)، نبود اسکرول افقی اضافی و صفر بودن خطای JS/404 روی دسکتاپ (1366×900)
۲) بررسی بصری واکنش‌گرا روی نمای موبایل (390×844) بدون بیرون‌زدگی افقی
۳) پرسونا معلم: ساخت آزمون چندبخشی متوسطه شامل:
   - سؤال ۱ (بخش هندسه مختصاتی با زمان مجاز ۱۰ دقیقه): ترسیم هندسی با نقطهٔ هدف B(0,0) و شرط زاویهٔ قائمه ∠ABC = 90°
   - سؤال ۲ (بخش اثبات مرحله‌ای): اثبات دوگامی با ارجاع زنده به نتیجهٔ گام قبل (carriesForward)
   - سؤال ۳ (بخش اثبات مرحله‌ای): چهارگزینه‌ای با تصحیح خودکار
   + ذخیره، بررسی تولید کد ۶حرفی، ذخیرهٔ مجدد و اثبات ثابت ماندن کد آزمون
۴) پرسونا معلم: باز کردن آزمون برای دانش‌آموزان در فهرست «آزمون‌ها» و خروج از حساب
۵) پرسونا دانش‌آموز: ورود با کد آزمون، بررسی عدم نشت کلید پاسخ در شبکه، و کلیک واقعی روی بوم مختصات (<canvas>) برای ثبت A(0,3)، B(0,0)، C(4,0) و مشاهدهٔ زندهٔ ∠ABC=90.0°
۶) پرسونا دانش‌آموز: نوشتن گام ۱ اثبات و مشاهدهٔ به‌روزرسانی آنی کارت «ارجاع به نتیجهٔ گام قبل» بالای گام ۲ + پاسخ به سؤال ۳
۷) انقضای زمان سرور بخش اول (`بخش هندسه مختصاتی`)، اثبات قفل شدن بازگشت به بخش منقضی‌شده، و ارسال نهایی پاسخ‌برگ
۸) پرسونا معلم: تصحیح خودکار ترسیم هندسی (۳ از ۳) و چهارگزینه‌ای (۳ از ۳)، ثبت نمره و بازخورد گام‌به‌گام اثبات (۲ + ۲ = ۴ از ۴) و نهایی‌سازی نمرهٔ کل ۱۰ از ۱۰
۹) پرسونا دانش‌آموز: مشاهدهٔ کارنامهٔ نهایی، نمرهٔ ۱۰ از ۱۰، نمره‌ها و بازخوردهای تفکیکی گام‌ها، و اطمینان از عدم نشت Rubric محرمانهٔ دبیر
"""
from __future__ import annotations

import argparse
import base64
import glob
import hashlib
import html
import json
import os
import re
import shutil
import sys
import tempfile
import threading
import time
import urllib.request
from http.server import SimpleHTTPRequestHandler
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import mock_supabase_server as mss  # noqa: E402


def build_chunked_site_out(dest_dir: Path, anon_key: str) -> None:
    """ساخت دقیقاً همان بستهٔ تکهتکه‌شدهٔ تولیدی (`site_out`) مطابق `.github/workflows/site.yml`."""
    site_dir = REPO_ROOT / "site"
    dest_dir.mkdir(parents=True, exist_ok=True)
    raw_html = (site_dir / "index.html").read_text(encoding="utf-8")
    old_marker = 'var SUPABASE_ANON_KEY = "…";'
    if old_marker in raw_html:
        raw_html = raw_html.replace(old_marker, f'var SUPABASE_ANON_KEY = "{anon_key}";', 1)
    (dest_dir / "index.html").write_text(raw_html, encoding="utf-8")

    shutil.copytree(site_dir / "pwa", dest_dir / "pwa", dirs_exist_ok=True)
    shutil.copytree(site_dir / "fonts", dest_dir / "fonts", dirs_exist_ok=True)
    for static_name in ("status.html", "terms.html", "privacy.html", "robots.txt", "sitemap.xml", "favicon.ico"):
        src_p = site_dir / static_name
        if src_p.exists():
            shutil.copy2(src_p, dest_dir / static_name)
    for pattern in ("engines.*.js", "panels.*.js"):
        for fp in glob.glob(str(site_dir / pattern)):
            shutil.copy2(fp, dest_dir / Path(fp).name)

    page_html = (dest_dir / "index.html").read_text(encoding="utf-8")
    styles = list(re.finditer(r"<style>([\s\S]*?)</style>", page_html))
    big_style = max(styles, key=lambda m: len(m.group(1)))
    css = big_style.group(1)
    chash = hashlib.sha256(css.encode("utf-8")).hexdigest()[:12]
    depth, mid, split_idx = 0, len(css) // 2, len(css)
    for idx, ch in enumerate(css):
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0 and idx >= mid:
                split_idx = idx + 1
                break
    (dest_dir / f"s0.{chash}.css").write_text(css[:split_idx], encoding="utf-8")
    (dest_dir / f"s1.{chash}.css").write_text(css[split_idx:], encoding="utf-8")
    page_html = (
        page_html[: big_style.start()]
        + f'<link rel="stylesheet" href="/s0.{chash}.css"><link rel="stylesheet" href="/s1.{chash}.css">'
        + page_html[big_style.end() :]
    )

    panel_files = sorted(glob.glob(str(site_dir / "panels.*.js")), key=os.path.getmtime)
    pjs = Path(panel_files[-1]).read_text(encoding="utf-8")
    phash = hashlib.sha256(pjs.encode("utf-8")).hexdigest()[:12]
    pstep = (len(pjs) + 4) // 5
    pchunks = [pjs[i : i + pstep] for i in range(0, len(pjs), pstep)]
    purls = []
    for idx, pc in enumerate(pchunks):
        pname = f"p{idx}.{phash}.js"
        purls.append("/" + pname)
        (dest_dir / pname).write_text(
            f"window.__PJ=window.__PJ||[];window.__PJ[{idx}]={json.dumps(pc, ensure_ascii=False)};\n",
            encoding="utf-8",
        )
    page_html = page_html.replace(
        "window.__PANELS_URL =",
        f"window.__PANELS_URLS = {json.dumps(purls)}; window.__PANELS_URL =",
        1,
    )

    scripts = list(re.finditer(r"<script>([\s\S]*?)</script>", page_html))
    big_script = max(scripts, key=lambda m: len(m.group(1)))
    js = big_script.group(1)
    jhash = hashlib.sha256(js.encode("utf-8")).hexdigest()[:12]
    jstep = (len(js) + 4) // 5
    jchunks = [js[i : i + jstep] for i in range(0, len(js), jstep)]
    stags = []
    tail = (
        "if(window.__CJ&&window.__CJ.length===5&&window.__CJ.filter(Boolean).length===5)"
        "{var _s=document.createElement('script');_s.textContent=window.__CJ.join('');"
        "delete window.__CJ;document.body.appendChild(_s);}"
    )
    for idx, jc in enumerate(jchunks):
        cname = f"c{idx}.{jhash}.js"
        stags.append(f'<script src="/{cname}"></script>')
        (dest_dir / cname).write_text(
            f"window.__CJ=window.__CJ||[];window.__CJ[{idx}]={json.dumps(jc, ensure_ascii=False)};{tail}\n",
            encoding="utf-8",
        )
    page_html = page_html[: big_script.start()] + "".join(stags) + page_html[big_script.end() :]
    (dest_dir / "index.html").write_text(page_html, encoding="utf-8")

    ihash = hashlib.sha256((dest_dir / "index.html").read_bytes()).hexdigest()[:12]
    sw_path = dest_dir / "pwa" / "sw.js"
    sw_src = sw_path.read_text(encoding="utf-8")
    if "__SW_VERSION__" in sw_src:
        sw_path.write_text(sw_src.replace("__SW_VERSION__", ihash), encoding="utf-8")


def start_readonly_site_server(site_dir: Path, mock_url: str, fallback_anon: str) -> tuple[Any, str]:
    """سرو کردن `site_out` بدون کوچک‌ترین تغییر روی دیسک (تزریق آدرس mock فقط در حافظهٔ پاسخ HTTP)."""

    class Handler(SimpleHTTPRequestHandler):
        def __init__(self, *args: Any, **kwargs: Any) -> None:
            super().__init__(*args, directory=str(site_dir), **kwargs)

        def do_GET(self) -> None:
            clean = self.path.split("?", 1)[0]
            if clean in ("/", "/index.html") or clean.endswith(".js"):
                target_file = site_dir / ("index.html" if clean in ("/", "/index.html") else clean.lstrip("/"))
                if target_file.is_file():
                    raw_txt = target_file.read_text(encoding="utf-8")
                    patched = re.sub(r"https://[a-z0-9-]+\.supabase\.co", mock_url, raw_txt)
                    if 'var SUPABASE_ANON_KEY = "…";' in patched:
                        patched = patched.replace(
                            'var SUPABASE_ANON_KEY = "…";',
                            f'var SUPABASE_ANON_KEY = "{fallback_anon}";',
                        )
                    payload = patched.encode("utf-8")
                    ctype = "text/html; charset=utf-8" if clean in ("/", "/index.html") else "application/javascript; charset=utf-8"
                    self.send_response(200)
                    self.send_header("Content-Type", ctype)
                    self.send_header("Content-Length", str(len(payload)))
                    self.end_headers()
                    self.wfile.write(payload)
                    return
            return super().do_GET()

        def log_message(self, fmt: str, *args: Any) -> None:
            return

    srv = mss.QuietThreadingHTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv, f"http://127.0.0.1:{srv.server_address[1]}"


def assert_layout_healthy(page: Any, label: str) -> dict[str, Any]:
    metrics = page.evaluate(
        """() => {
            const docEl = document.documentElement;
            return {
                dir: docEl.getAttribute('dir'),
                lang: docEl.getAttribute('lang'),
                innerWidth: window.innerWidth,
                innerHeight: window.innerHeight,
                scrollWidth: docEl.scrollWidth,
                bodyTextLen: (() => {
                    /* V352.1 — وقتی موتور چاپ/کارنامه باز است، قاعدهٔ V179
                       پوستهٔ زیرین را visibility:hidden می‌کند و innerText
                       متن پنهان را نمی‌شمارد. پیش‌تر این سنجه تنها به این
                       دلیل پاس می‌شد که یک پیام شناور اتفاقاً روی صفحه بود؛
                       با محو شدن آن، صفحهٔ سالم «خالی» گزارش می‌شد.
                       حالا در حالت موتور، متن خودِ اورلی و سند iframe
                       شمرده می‌شود — یعنی همان چیزی که کاربر می‌بیند. */
                    const body = document.body;
                    if (!body.classList.contains('engine-open')) {
                        return (body.innerText || '').trim().length;
                    }
                    const ov = document.querySelector('.engine-bg');
                    let n = ((ov && ov.innerText) || '').trim().length;
                    const fr = ov && ov.querySelector('iframe');
                    try {
                        const d = fr && fr.contentDocument;
                        if (d && d.body) n += (d.body.innerText || '').trim().length;
                    } catch (e) { /* اگر هم‌مبدأ نبود، همان متن اورلی بسنده است */ }
                    return n;
                })()
            };
        }"""
    )
    assert metrics["dir"] == "rtl", f"[{label}] جهت صفحه باید rtl باشد اما {metrics['dir']} بود"
    assert metrics["lang"] == "fa", f"[{label}] زبان صفحه باید fa باشد اما {metrics['lang']} بود"
    assert metrics["scrollWidth"] <= metrics["innerWidth"] + 4, (
        f"[{label}] بیرون‌زدگی افقی تشخیص داده شد: scrollWidth={metrics['scrollWidth']} > innerWidth={metrics['innerWidth']}"
    )
    assert metrics["bodyTextLen"] > 20, f"[{label}] محتوای متنی صفحه خالی است"
    return metrics


def click_visible_button(page: Any, target: str, substring: bool = False) -> None:
    page.evaluate(
        """([want, sub]) => {
            const radial = document.querySelector('.dk-radial, .m-radial-bg');
            const pool = radial ? [...radial.querySelectorAll('button')] : [...document.querySelectorAll('button')];
            const btn = pool.find(b => {
                if (!radial && b.offsetParent === null && getComputedStyle(b).position !== 'fixed') return false;
                const cand = [
                    (b.innerText || '').trim().replace(/\\s+/g, ' '),
                    (b.getAttribute('aria-label') || '').trim(),
                    (b.getAttribute('title') || '').trim()
                ];
                return sub ? cand.some(c => c.includes(want)) : cand.some(c => c === want);
            });
            if (!btn) {
                const avail = [...document.querySelectorAll('button')]
                    .filter(b => b.offsetParent !== null)
                    .map(b => (b.innerText || b.getAttribute('aria-label') || b.title || '').trim())
                    .filter(Boolean);
                throw new Error('Button not found: ' + want + ' | visible=' + JSON.stringify(avail.slice(0, 30)));
            }
            btn.scrollIntoView({ block: 'center' });
            btn.click();
        }""",
        [target, substring],
    )


def set_question_text(page: Any, value: str) -> None:
    ta = page.locator("textarea[placeholder*='متن سؤال']").first
    ta.wait_for(state="attached")
    ta.evaluate(
        """(el, val) => {
            el.value = val;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            const ce = el.parentElement?.querySelector('[contenteditable="true"]');
            if (ce && !ce.textContent.trim()) {
                ce.textContent = val;
                ce.dispatchEvent(new Event('input', { bubbles: true }));
            }
        }""",
        value,
    )


def set_mc_options(page: Any, values: list[str]) -> None:
    page.evaluate(
        """(vals) => {
            const fields = [...document.querySelectorAll('textarea, input[type="text"]')]
                .filter(el => (el.placeholder || '').includes('گزینه'));
            vals.forEach((v, idx) => {
                if (fields[idx]) {
                    fields[idx].value = v;
                    fields[idx].dispatchEvent(new Event('input', { bubbles: true }));
                    const ce = fields[idx].parentElement?.querySelector('[contenteditable="true"]');
                    if (ce && !ce.textContent.trim()) {
                        ce.textContent = v;
                        ce.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }
            });
        }""",
        values,
    )


def fill_labeled_field(root: Any, label_sub: str, value: str) -> None:
    target_loc = root.locator("body") if hasattr(root, "goto") else root
    target_loc.evaluate(
        """(container, [want, val]) => {
            const fields = [...container.querySelectorAll('.field')];
            const match = fields.find(f => (f.querySelector('label')?.textContent || '').trim().includes(want));
            if (!match) {
                const avail = fields.map(f => (f.querySelector('label')?.textContent || '').trim());
                throw new Error('Field label not found: ' + want + ' | available=' + JSON.stringify(avail));
            }
            const inp = match.querySelector('input, textarea');
            if (!inp) throw new Error('Input not found in field: ' + want);
            inp.scrollIntoView({ block: 'center' });
            inp.value = val;
            inp.dispatchEvent(new Event('input', { bubbles: true }));
            inp.dispatchEvent(new Event('change', { bubbles: true }));
        }""",
        [label_sub, value],
    )


def select_labeled_option(root: Any, label_sub: str, option_value: str) -> None:
    target_loc = root.locator("body") if hasattr(root, "goto") else root
    target_loc.evaluate(
        """(container, [want, val]) => {
            const fields = [...container.querySelectorAll('.field')];
            const match = fields.find(f => (f.querySelector('label')?.textContent || '').trim().includes(want));
            if (!match) throw new Error('Select field not found: ' + want);
            const sel = match.querySelector('select');
            if (!sel) throw new Error('Select element missing: ' + want);
            sel.value = val;
            sel.dispatchEvent(new Event('change', { bubbles: true }));
        }""",
        [label_sub, option_value],
    )


def click_canvas_math_point(page: Any, canvas_loc: Any, math_x: float, math_y: float) -> None:
    canvas_loc.scroll_into_view_if_needed()
    box = canvas_loc.bounding_box()
    assert box is not None, "کادر بوم مختصات یافت نشد"
    px = (30.0 + (math_x + 5.0) / 10.0 * 580.0) / 640.0 * box["width"]
    py = (30.0 + (5.0 - math_y) / 10.0 * 280.0) / 340.0 * box["height"]
    page.mouse.click(box["x"] + px, box["y"] + py)


def write_visual_reports(out_dir: Path, steps: list[dict[str, Any]], meta: dict[str, Any]) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    summary_payload = {"ok": all(s["passed"] for s in steps), "meta": meta, "steps": steps}
    (out_dir / "summary.json").write_text(
        json.dumps(summary_payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    md_lines = [
        "### 🧪 گزارش تست خودکار بصری و تعاملی وب (`Chromium RTL E2E`)",
        "",
        f"- **وضعیت کل:** {'✅ تمام سناریوها سبز (`PASS`)' if summary_payload['ok'] else '❌ شکست (`FAIL`)'}",
        f"- **تعداد سناریوهای بصری:** `{len(steps)}` مرحله با تصویر واقعی (`PNG`)",
        f"- **خطاهای جاوااسکریپت مرورگر (`pageerror`):** `{len(meta.get('page_errors', []))}`",
        f"- **پاسخ‌های خطای دارایی‌های سایت (`HTTP >= 400`):** `{len(meta.get('asset_failures', []))}`",
        f"- **کد آزمون تولید و تست‌شده:** `{meta.get('exam_code', '—')}`",
        "",
        "| ردیف | سناریو | نما (`Viewport`) | زمان | وضعیت | تصویر |",
        "| :---: | --- | :---: | :---: | :---: | :---: |",
    ]
    for idx, st in enumerate(steps, 1):
        icon = "✅" if st["passed"] else "❌"
        md_lines.append(
            f"| {idx} | {st['title']} | `{st['viewport']}` | `{st['duration_ms']}ms` | {icon} | `{st['screenshot']}` |"
        )
    (out_dir / "summary.md").write_text("\n".join(md_lines) + "\n", encoding="utf-8")

    cards_html = []
    for idx, st in enumerate(steps, 1):
        shot_path = out_dir / st["screenshot"]
        b64 = base64.b64encode(shot_path.read_bytes()).decode("ascii") if shot_path.exists() else ""
        badge_cls = "ok" if st["passed"] else "fail"
        cards_html.append(
            f"""
            <article class="shot-card">
              <header>
                <span class="num">{idx}</span>
                <div class="grow">
                  <h3>{html.escape(st['title'])}</h3>
                  <small>{html.escape(st['detail'])}</small>
                </div>
                <span class="badge {badge_cls}">{'PASS' if st['passed'] else 'FAIL'} · {st['viewport']} · {st['duration_ms']}ms</span>
              </header>
              <img src="data:image/png;base64,{b64}" alt="{html.escape(st['title'])}">
            </article>
            """
        )

    report_html = f"""<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<title>گزارش بصری تست خودکار وب — آزمون آنلاین</title>
<style>
  body {{ font-family: system-ui, Tahoma, sans-serif; background: #f3f5fa; color: #1d2433; margin: 0; padding: 24px; }}
  .wrap {{ max-width: 1180px; margin: 0 auto; }}
  h1 {{ margin: 0 0 8px; color: #3f2b78; }}
  .summary {{ background: #fff; border-radius: 14px; padding: 16px 20px; box-shadow: 0 4px 16px rgba(0,0,0,.06); margin-bottom: 20px; display: flex; gap: 20px; flex-wrap: wrap; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(500px, 1fr)); gap: 18px; }}
  .shot-card {{ background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,.06); border: 1px solid #e2e7f1; }}
  .shot-card header {{ display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-bottom: 1px solid #eef1f7; }}
  .num {{ width: 28px; height: 28px; border-radius: 50%; background: #5B3FA3; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-weight: bold; flex-shrink: 0; }}
  .grow {{ flex: 1; }}
  .shot-card h3 {{ margin: 0; font-size: 15px; }}
  .shot-card small {{ color: #5b6577; font-size: 12px; }}
  .badge {{ padding: 4px 10px; border-radius: 99px; font-size: 12px; font-weight: bold; direction: ltr; }}
  .badge.ok {{ background: #e6f7ee; color: #137333; }}
  .badge.fail {{ background: #fce8e6; color: #c5221f; }}
  .shot-card img {{ width: 100%; display: block; background: #f8f9fc; }}
</style>
</head>
<body>
<div class="wrap">
  <h1>گزارش بصری و تعاملی تست خودکار وب (Chromium E2E)</h1>
  <div class="summary">
    <div><b>نتیجهٔ کل:</b> {'✅ موفق (PASS)' if summary_payload['ok'] else '❌ ناموفق (FAIL)'}</div>
    <div><b>سناریوها:</b> {len(steps)} مرحله</div>
    <div><b>کد آزمون آزمایشی:</b> <code>{html.escape(str(meta.get('exam_code', '')))}</code></div>
    <div><b>خطای JS مرورگر:</b> {len(meta.get('page_errors', []))}</div>
    <div><b>خطای شبکه دارایی‌ها:</b> {len(meta.get('asset_failures', []))}</div>
  </div>
  <div class="grid">{''.join(cards_html)}</div>
</div>
</body>
</html>
"""
    (out_dir / "index.html").write_text(report_html, encoding="utf-8")


def run_suite(site_dir_arg: str | None, out_dir: Path) -> int:
    from playwright.sync_api import sync_playwright

    out_dir.mkdir(parents=True, exist_ok=True)
    mock_srv, mock_state = mss.start_server_in_thread("127.0.0.1", 0)
    mock_url = f"http://127.0.0.1:{mock_srv.server_address[1]}"
    anon_key = mss.make_public_anon_key()

    temp_site_ctx = None
    if site_dir_arg:
        site_dir = Path(site_dir_arg).resolve()
        before_hash = hashlib.sha256((site_dir / "index.html").read_bytes()).hexdigest()
    else:
        temp_site_ctx = tempfile.TemporaryDirectory(prefix="v333_site_out_")
        site_dir = Path(temp_site_ctx.name)
        build_chunked_site_out(site_dir, anon_key)
        before_hash = hashlib.sha256((site_dir / "index.html").read_bytes()).hexdigest()

    web_srv, web_url = start_readonly_site_server(site_dir, mock_url, anon_key)

    steps: list[dict[str, Any]] = []
    page_errors: list[str] = []
    asset_failures: list[str] = []
    leaked_student_payload_keys: list[str] = []
    exam_code = ""
    exam_id = ""

    def record_step(page: Any, shot_name: str, title: str, detail: str, viewport_str: str, t0: float) -> None:
        page.evaluate(
            """() => {
                const items = [...document.querySelectorAll('#toasts > *')];
                items.slice(0, -1).forEach(n => n.remove());
            }"""
        )
        assert_layout_healthy(page, title)
        page.screenshot(path=str(out_dir / shot_name), full_page=False, animations="disabled")
        steps.append(
            {
                "title": title,
                "detail": detail,
                "viewport": viewport_str,
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": shot_name,
            }
        )
        print(f"  [PASS] {shot_name} — {title} ({steps[-1]['duration_ms']}ms)", flush=True)

    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)

            # --------------------------------------------------------
            # ۱) نمای دسکتاپ RTL (1366×900) — صفحهٔ فرود و ورود
            # --------------------------------------------------------
            t0 = time.monotonic()
            desktop_ctx = browser.new_context(
                viewport={"width": 1366, "height": 900},
                locale="fa-IR",
                service_workers="block",
            )
            page = desktop_ctx.new_page()
            page.set_default_timeout(8000)
            page.on("pageerror", lambda err: page_errors.append(str(err)))

            def on_response(resp: Any) -> None:
                if resp.url.startswith(web_url) and resp.status >= 400 and not resp.url.endswith("/favicon.ico"):
                    asset_failures.append(f"{resp.status} {resp.url}")
                if "/rest/v1/rpc/get_exam_for_student" in resp.url:
                    try:
                        body_txt = resp.text()
                        for forbidden in (
                            '"constructionTargets"',
                            '"constructionConstraints"',
                            '"partRubrics"',
                            '"rubric"',
                            '"correctOption"',
                        ):
                            if forbidden in body_txt:
                                leaked_student_payload_keys.append(forbidden)
                    except Exception:
                        pass

            page.on("response", on_response)
            page.goto(web_url + "/", wait_until="domcontentloaded")
            page.locator("input[type='text']").first.wait_for()
            record_step(
                page,
                "01_landing_desktop.png",
                "صفحهٔ فرود و کارت ورود در نمای دسکتاپ راست‌به‌چپ",
                "بررسی بارگذاری تکههای CSS/JS، چیدمان RTL و نبود خطای کنسول",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۲) نمای موبایل RTL (390×844) — بررسی واکنش‌گرا
            # --------------------------------------------------------
            t0 = time.monotonic()
            mobile_ctx = browser.new_context(
                viewport={"width": 390, "height": 844},
                locale="fa-IR",
                is_mobile=True,
                has_touch=True,
                service_workers="block",
            )
            mpage = mobile_ctx.new_page()
            mpage.set_default_timeout(10000)
            mpage.on("pageerror", lambda err: page_errors.append(str(err)))
            mpage.goto(web_url + "/", wait_until="domcontentloaded")
            mobile_auth_btn = mpage.get_by_role("button", name="ورود / ثبت‌نام")
            mobile_auth_btn.wait_for()
            mobile_auth_btn.click()
            mpage.locator("input[type='password']").first.wait_for()
            mpage.locator("input[type='password']").first.scroll_into_view_if_needed()
            record_step(
                mpage,
                "02_landing_mobile.png",
                "صفحهٔ خوش‌آمد و فرم ورود واکنش‌گرا در نمای گوشی هوشمند",
                "باز کردن پنجرهٔ «ورود / ثبت‌نام» موبایل و اطمینان از نبود اسکرول افقی در عرض ۳۹۰ پیکسل",
                "390×844",
                t0,
            )
            mobile_ctx.close()

            # --------------------------------------------------------
            # ۳) ورود معلم + بررسی خلوتی کامل سازنده + ساخت آزمون ۳ سؤالی (تشریحی + عددی + چندگزینه‌ای)
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_teacher")
            page.locator("input[type='password']").first.fill("TestPass1234!")
            page.locator("input[type='password']").first.press("Enter")
            page.locator("button[aria-label='افزودن']").wait_for()

            # بررسی سازندهٔ «آزمون چاپی» (V334.3): داشتن «قالب متن سؤال» و «فضای پاسخ در برگهٔ چاپی» و نداشتن اجزای آنلاین
            page.evaluate("() => window.ExamSite.go('builder', { mode: 'print', fresh: true })")
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(100)
            click_visible_button(page, "تشریحی", substring=True)
            set_question_text(page, "قضیهٔ فیثاغورس را بیان و برای مثلث قائم‌الزاویهٔ ABC اثبات کنید.")
            print_dom = page.evaluate("() => document.body.innerText")
            assert "قالب متن سؤال" in print_dom, "قالب متن سؤال باید در سازندهٔ چاپی دیده شود"
            assert "فضای پاسخ در برگهٔ چاپی" in print_dom, "فضای پاسخ چاپی باید در سازندهٔ چاپی دیده شود"
            assert "سؤال تشریحی به‌صورت دستی تصحیح می‌شود." not in print_dom, "پیام تصحیح آنلاین نباید در سازندهٔ چاپی باشد"
            assert "پاسخ تصویری / تختهٔ دانش‌آموز" not in print_dom, "پاسخ تصویری آنلاین نباید در سازندهٔ چاپی باشد"
            assert page.locator(".b-tools button[aria-label='صوت سؤال']").count() == 0, "دکمهٔ صوت سؤال نباید در سازندهٔ چاپی باشد"
            page.screenshot(path=str(out_dir / "03a_print_builder_clean.png"))

            # بازگشت به سازندهٔ «آزمون آنلاین جدید» (V334.3): نداشتن هیچ‌یک از بخش‌های کاغذی/چاپی
            page.evaluate("() => window.ExamSite.go('builder', { fresh: true })")
            page.get_by_role("button", name="ذخیره", exact=True).wait_for()

            exam_title = "آزمون جامع ریاضی و هندسه (رابط ساده)"
            fill_labeled_field(page, "عنوان آزمون", exam_title)
            fill_labeled_field(page, "درس", "ریاضی ۱")
            fill_labeled_field(page, "مدت (دقیقه)", "30")

            # سؤال ۱: تشریحی (۴ نمره) + بررسی حذف ۱۰۰٪ هر ۷ بخش اضافی در کارت سؤال و تنظیمات آزمون
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "تشریحی", substring=True)
            set_question_text(
                page,
                "در مثلث قائم‌الزاویهٔ ABC با اضلاع قائمهٔ AB=3 و BC=4، با استفاده از قضیهٔ فیثاغورس طول وتر AC را مرحله‌به‌مرحله محاسبه کنید.",
            )

            forbidden_ui_labels = [
                "بخش آزمون / صورت مشترک",
                "فصل یا مبحث",
                "سطح دشواری",
                "هدف یادگیری",
                "متن/مسئلهٔ مشترک",
                "زیرسؤال‌های مستقل",
                "افزودن زیربخش",
                "حالت پاسخ",
                "Rubric دبیر",
                "زمان مجاز هر بخش",
            ]
            builder_dom_text = page.evaluate("() => document.body.innerText")
            for bad_lbl in forbidden_ui_labels + ["قالب متن سؤال", "فضای پاسخ در برگهٔ چاپی", "چیدمان گزینه‌ها در برگهٔ چاپی"]:
                assert bad_lbl not in builder_dom_text, f"فیلد غیرمربوط نباید در سازندهٔ آزمون آنلاین دیده شود: {bad_lbl}"
            assert "سؤال تشریحی به‌صورت دستی تصحیح می‌شود." in builder_dom_text, "توضیح تصحیح دستی باید در آزمون آنلاین باشد"
            assert "پاسخ تصویری / تختهٔ دانش‌آموز" in builder_dom_text, "پاسخ تصویری دانش‌آموز باید در آزمون آنلاین باشد"
            assert page.locator(".b-section-durations").count() == 0, "کادر زمان بخش نباید در تنظیمات آزمون وجود داشته باشد!"

            # تنظیم بارم سؤال ۱ = ۴
            page.evaluate(
                """() => {
                    const scoreFields = [...document.querySelectorAll('.field')].filter(f => f.querySelector('label')?.textContent.trim() === 'بارم');
                    const mainScore = scoreFields[scoreFields.length - 1];
                    if (mainScore) {
                        const i = mainScore.querySelector('input');
                        i.value = '4';
                        i.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )

            # بررسی پنجرهٔ «بانک سؤال» در سازنده (حذف فیلترهای فصل/مبحث، سطح دشواری و هدف یادگیری)
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "بانک", substring=True)
            page.locator(".b-bank-modal").wait_for()
            bank_dom_text = page.locator(".b-bank-modal").inner_text()
            for bad_bank in ("همهٔ فصل‌ها/مباحث", "همهٔ سطوح", "هدف یادگیری"):
                assert bad_bank not in bank_dom_text, f"فیلتر حذف‌شده نباید در پنجرهٔ بانک سؤال باشد: {bad_bank}"
            page.locator(".b-bank-modal button.x").click()
            page.wait_for_timeout(120)

            # افزودن سؤال ۲: عددی (۳ نمره، پاسخ درست = ۵)
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "عددی", substring=True)
            set_question_text(
                page,
                "در همان مثلث قائم‌الزاویه با اضلاع قائمهٔ ۳ و ۴، اندازهٔ وتر برابر چند واحد است؟",
            )
            page.evaluate(
                """() => {
                    const fields = [...document.querySelectorAll('.field')];
                    const numField = fields.find(f => (f.querySelector('label')?.textContent || '').includes('پاسخ عددی'));
                    if (numField) {
                        const inp = numField.querySelector('input');
                        inp.value = '5';
                        inp.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                    const scoreField = fields.find(f => f.querySelector('label')?.textContent.trim() === 'بارم');
                    if (scoreField) {
                        const si = scoreField.querySelector('input');
                        si.value = '3';
                        si.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )

            # افزودن سؤال ۳: چندگزینه‌ای (۳ نمره، گزینهٔ درست = «۶»)
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "چندگزینه‌ای", substring=True)
            set_question_text(
                page,
                "مساحت مثلث قائم‌الزاویه با اضلاع قائمهٔ ۳ و ۴ چند واحد مربع است؟",
            )
            set_mc_options(page, ["۵", "۶", "۷", "۱۲"])
            page.evaluate(
                """() => {
                    const radios = [...document.querySelectorAll('input[type=radio]')];
                    if (radios[1]) { radios[1].checked = true; radios[1].dispatchEvent(new Event('change', { bubbles: true })); radios[1].click(); }
                    const scoreField = [...document.querySelectorAll('.field')].find(f => f.querySelector('label')?.textContent.trim() === 'بارم');
                    if (scoreField) {
                        const i = scoreField.querySelector('input');
                        i.value = '3';
                        i.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )

            # بررسی چیدمان V334.1 + V334.3 روی سؤال چندگزینه‌ای آنلاین (گزینه‌ها بلافاصله زیر ابزارها، بارم در سربرگ، بدون قالب چاپی)
            mc_geom = page.evaluate(
                """() => {
                    const head = document.querySelector('.b-editor .b-qhead .b-qscore').getBoundingClientRect();
                    const tools = document.querySelector('.b-editor .b-tools').getBoundingClientRect();
                    const typeBox = document.querySelector('.b-editor .b-type').getBoundingClientRect();
                    const fmtEl = document.querySelector('.b-editor .b-sec-fmt');
                    return { headBottom: head.bottom, toolsTop: tools.top, toolsBottom: tools.bottom, typeTop: typeBox.top, hasPrintFmt: !!fmtEl };
                }"""
            )
            assert mc_geom["hasPrintFmt"] is False, f"قالب چاپی نباید در آزمون آنلاین رندر شود: {mc_geom}"
            assert mc_geom["headBottom"] <= mc_geom["toolsTop"] + 2, f"بارم باید در سربرگ کارت بالای ابزارها باشد: {mc_geom}"
            assert mc_geom["toolsBottom"] <= mc_geom["typeTop"] + 2, f"گزینه‌ها باید زیر نوار ابزار باشد: {mc_geom}"

            # بررسی موقت نوع «جورکردنی» و تأیید قرارگیری ستون‌های جورکردنی بلافاصله زیر ابزارها، سپس بازگرداندن به چندگزینه‌ای
            page.evaluate(
                """() => {
                    const matchChip = [...document.querySelectorAll('.b-types .chip')].find(c => c.textContent.includes('جور'));
                    if (matchChip) matchChip.click();
                }"""
            )
            page.wait_for_timeout(100)
            match_geom = page.evaluate(
                """() => {
                    const tools = document.querySelector('.b-editor .b-tools').getBoundingClientRect();
                    const typeBox = document.querySelector('.b-editor .b-type').getBoundingClientRect();
                    return {
                        text: document.querySelector('.b-editor .b-type').innerText,
                        toolsBottom: tools.bottom,
                        typeTop: typeBox.top,
                        hasPrintFmt: !!document.querySelector('.b-editor .b-sec-fmt')
                    };
                }"""
            )
            assert "ستون راست" in match_geom["text"], f"ستون جورکردنی نمایش داده نشد: {match_geom}"
            assert match_geom["toolsBottom"] <= match_geom["typeTop"] + 2, f"ستون جورکردنی باید زیر ابزارها باشد: {match_geom}"
            assert match_geom["hasPrintFmt"] is False, f"قالب چاپی نباید در جورکردنی آنلاین باشد: {match_geom}"

            # بازگرداندن سؤال ۳ به چندگزینه‌ای با همان گزینه‌ها و پاسخ درست
            page.evaluate(
                """() => {
                    const mcChip = [...document.querySelectorAll('.b-types .chip')].find(c => c.textContent.includes('چند'));
                    if (mcChip) mcChip.click();
                }"""
            )
            set_mc_options(page, ["۵", "۶", "۷", "۱۲"])
            page.evaluate(
                """() => {
                    const radios = [...document.querySelectorAll('input[type=radio]')];
                    if (radios[1]) { radios[1].checked = true; radios[1].dispatchEvent(new Event('change', { bubbles: true })); radios[1].click(); }
                    const scoreField = [...document.querySelectorAll('.field')].find(f => f.querySelector('label')?.textContent.trim() === 'بارم');
                    if (scoreField) {
                        const i = scoreField.querySelector('input');
                        i.value = '3';
                        i.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )

            # ماندن روی سؤال ۳ چندگزینه‌ای برای ثبت اسکرین‌شات واضح که «گزینه‌ها» بلافاصله زیر ابزارها دیده می‌شود
            page.evaluate("window.scrollTo(0, 0)")
            record_step(
                page,
                "03_teacher_builder_clean.png",
                "سازندهٔ آزمون V334.1 — قرارگیری بلافاصلهٔ گزینه‌ها/ستون جورکردنی زیر نوار ابزارها",
                "تأیید مختصات پیکسل واقعی: نوار ابزارها ← گزینه‌ها/ستون جورکردنی ← قالب متن سؤال",
                "1366×900",
                t0,
            )


            # --------------------------------------------------------
            # ۳ب) V352.4 — تراشهٔ ابزار کد نباید «کد آزمون» شمرده شود
            #     تداخل کلاس: تراشه کلاس «code» دارد و شنوندهٔ V234 در app.js
            #     هر .code را کد آزمون می‌گیرد. این گام جلوی بازگشت آن را می‌گیرد.
            # --------------------------------------------------------
            t0 = time.monotonic()
            desktop_ctx.grant_permissions(["clipboard-read", "clipboard-write"])
            chip_code = '<?php\n\nfunction greet(string $name): string {\n    return "سلام " . $name;\n}\n\necho greet("دنیا");'
            chip_fence = "```php\n" + chip_code + "\n```"
            saved_q = page.evaluate(
                """() => {
                    const ta = document.querySelector("textarea[placeholder*='متن سؤال']");
                    return ta ? ta.value : null;
                }"""
            )
            set_question_text(page, "متن سؤال با ابزار کد\n" + chip_fence)
            page.wait_for_selector(".b-rich .b-chip.code .cx-block", timeout=8000)
            page.wait_for_timeout(400)

            # ۱) کلیک اول نباید چیزی کپی کند و نباید توست «کد آزمون» بدهد
            page.evaluate("() => navigator.clipboard.writeText('__SENTINEL__')")
            page.wait_for_timeout(150)
            page.locator(".b-rich .b-chip.code .cx-code").first.click()
            page.wait_for_timeout(350)
            after_click = page.evaluate("() => navigator.clipboard.readText()")
            assert after_click == "__SENTINEL__", (
                "کلیک روی تراشهٔ کد در کلیپ‌بورد نوشت — تداخل کلاس .code برگشته: " + after_click[:90]
            )
            chip_toasts = page.evaluate("() => (document.getElementById('toasts') || {}).innerText || ''")
            assert "کد آزمون" not in chip_toasts, "توست «کد آزمون» روی تراشهٔ ابزار کد ظاهر شد"
            assert "sel" in page.eval_on_selector(".b-rich .b-chip.code", "n => n.className"), "تراشه انتخاب نشد"

            # ۲) Ctrl+C داخل سازنده = قالب اصلی، بدون کروم
            page.keyboard.press("Control+c")
            page.wait_for_timeout(300)
            raw_clip = page.evaluate("() => navigator.clipboard.readText()")
            assert raw_clip.strip() == chip_fence, "کپیِ سازنده قالب اصلی نداد: " + raw_clip[:90]

            # ۳) دکمهٔ کپی: فقط کد تمیز، بدون انتخاب و بدون باز کردن ویرایشگر
            page.evaluate(
                """() => {
                    const c = document.querySelector('.b-rich .b-chip.code');
                    c.classList.remove('sel');
                    document.getSelection().removeAllRanges();
                    return navigator.clipboard.writeText('__SENTINEL__');
                }"""
            )
            page.wait_for_timeout(150)
            page.locator(".b-rich .b-chip.code .cx-copy").first.click()
            page.wait_for_timeout(350)
            btn_clip = page.evaluate("() => navigator.clipboard.readText()")
            assert btn_clip == chip_code, "دکمهٔ کپی کد تمیز نداد: " + btn_clip[:90]
            assert page.locator(".cx-editor-bg").count() == 0, "دکمهٔ کپی پنجرهٔ ویرایش را باز کرد"
            assert "sel" not in page.eval_on_selector(".b-rich .b-chip.code", "n => n.className"), "دکمهٔ کپی تراشه را انتخاب کرد"

            # بازگرداندن متن سؤال تا بقیهٔ مجموعه دست‌نخورده بماند
            if saved_q is not None:
                set_question_text(page, saved_q)
                page.wait_for_timeout(250)
            page.evaluate("window.scrollTo(0, 0)")
            record_step(
                page,
                "03b_code_chip_no_hijack.png",
                "تراشهٔ ابزار کد — کلیک اول کپی نمی‌کند، کپی سازنده قالب اصلی می‌دهد، دکمهٔ کپی فقط کد تمیز",
                "مهار تداخل کلاس .code میان «کد آزمون» (V234) و تراشهٔ ابزار کد (V350)",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۴) ذخیرهٔ دوبار (اثبات ثبات کد ۶حرفی) + باز کردن آزمون برای دانش‌آموزان
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.get_by_role("button", name="ذخیره", exact=True).click()
            page.locator(".modal button", has_text="ذخیره").click()
            page.wait_for_function("() => document.querySelectorAll('.modal button').length > 0")
            page.wait_for_timeout(400)
            if page.locator(".modal button").count() > 0:
                page.locator(".modal button").first.click()

            assert len(mock_state.exams) == 1, "آزمون باید در سرور ذخیره شده باشد"
            saved_exam = next(iter(mock_state.exams.values()))
            exam_id = saved_exam["id"]
            exam_code = saved_exam["code"]
            assert len(exam_code) == 6, f"کد آزمون نامعتبر: {exam_code}"

            page.get_by_role("button", name="ذخیره", exact=True).click()
            page.locator(".modal button", has_text="ذخیره").click()
            page.wait_for_timeout(350)
            if page.locator(".modal button").count() > 0:
                page.locator(".modal button").first.click()
            assert mock_state.exams[exam_id]["code"] == exam_code, "کد آزمون پس از ویرایش نباید تغییر کند!"

            page.evaluate("() => window.ExamSite.go('exams')")
            page.get_by_text(exam_title).first.wait_for()
            open_btn = page.locator("button[title*='باز'], button[aria-label*='باز'], button:has-text('باز کردن'), button:has-text('بازکردن')").first
            if open_btn.count() > 0:
                open_btn.click()
            else:
                page.evaluate(
                    """() => {
                        const btns = [...document.querySelectorAll('button')];
                        const b = btns.find(x => (x.title || x.innerText || '').includes('باز'));
                        if (b) b.click();
                    }"""
                )
            page.locator("button[title='بستن آزمون']").first.wait_for()
            assert mock_state.exams[exam_id]["is_open"] is True, "وضعیت آزمون باید باز (is_open=true) شود"
            record_step(
                page,
                "04_teacher_open_exam.png",
                "داشبورد معلم — باز کردن آزمون و تثبیت کد ۶حرفی",
                f"کد آزمون {exam_code} بدون تغییر حفظ شد و وضعیت آزمون به «باز» تغییر یافت",
                "1366×900",
                t0,
            )

            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۵) ورود دانش‌آموز + ورود با کد ۶حرفی + پاسخ به سؤال ۱ تشریحی
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_student")
            page.locator("input[type='password']").first.fill("StudentPass123!")
            page.locator("input[type='password']").first.press("Enter")
            page.wait_for_function("() => !document.querySelector('input[type=password]')")

            page.evaluate("() => window.ExamSite.go('join')")
            code_inp = page.locator("input[placeholder*='کد']").first
            code_inp.wait_for()
            code_inp.fill(exam_code)
            click_visible_button(page, "ورود به آزمون")

            essay_inp = page.locator("textarea, .st-input").first
            essay_inp.wait_for()
            assert not leaked_student_payload_keys, f"نشت کلید محرمانه در شبکه: {leaked_student_payload_keys}"
            essay_inp.fill("طبق قضیهٔ فیثاغورس داریم AC² = AB² + BC² = ۹ + ۱۶ = ۲۵ و در نتیجه AC = ۵ واحد است.")

            record_step(
                page,
                "05_student_essay_answer.png",
                "پاسخ‌گویی دانش‌آموز — ثبت پاسخ تشریحی بدون نشت کلید محرمانه",
                "ورود به آزمون با کد ۶حرفی و ثبت کامل استدلال تشریحی سؤال اول",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۶) رفتن به سؤال ۲ (عددی) و ثبت پاسخ عددی ۵
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.get_by_role("button", name=re.compile(r"بعدی")).first.click()
            page.get_by_text("اندازهٔ وتر").first.wait_for()
            num_inp = page.locator("input[type='number'], input[inputmode='decimal'], .st-input, input[type='text']").first
            num_inp.fill("5")

            record_step(
                page,
                "06_student_numeric_answer.png",
                "پاسخ‌گویی دانش‌آموز — ثبت پاسخ عددی سؤال دوم",
                "حرکت روان بین سؤالات و ثبت مقدار عددی ۵ برای سؤال دوم",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۷) پاسخ به سؤال ۳ چندگزینه‌ای + ارسال نهایی + بررسی نمره‌دهی خودکار و صدور رسید
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.get_by_role("button", name=re.compile(r"بعدی")).first.click()
            page.get_by_text("مساحت مثلث").first.wait_for()
            page.get_by_text("۶").first.click()

            submit_btn = page.get_by_role("button", name=re.compile(r"ثبت نهایی|ارسال نهایی|پایان آزمون|ارسال پاسخ")).first
            submit_btn.click()
            page.wait_for_timeout(250)
            confirm_btns = page.locator(".modal button").filter(has_text=re.compile(r"ارسال|ثبت|تأیید"))
            if confirm_btns.count() > 0:
                confirm_btns.first.click()

            page.wait_for_function("() => document.body.innerText.includes('رسید') || document.body.innerText.includes('ثبت شد')", timeout=8000)
            assert len(mock_state.answers) == 1, "پاسخ دانش‌آموز باید در سرور ثبت شده باشد"
            ans_rec = next(iter(mock_state.answers.values()))
            assert ans_rec["grades"][1] == 3.0, f"نمرهٔ خودکار عددی باید ۳ باشد اما {ans_rec['grades'][1]} بود"
            assert ans_rec["grades"][2] == 3.0, f"نمرهٔ خودکار چهارگزینه‌ای باید ۳ باشد اما {ans_rec['grades'][2]} بود"

            record_step(
                page,
                "07_student_submit_receipt.png",
                "ثبت نهایی آزمون، نمره‌دهی خودکار فوری (۶ از ۶ خودکار) و صدور کد رسید",
                "تصحیح خودکار فوری سؤال عددی (۳/۳) و چهارگزینه‌ای (۳/۳) و نمایش رسید ثبت نهایی",
                "1366×900",
                t0,
            )

            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۸) ورود مجدد معلم + تصحیح سؤال تشریحی + ثبت بازخورد و نمرهٔ نهایی ۱۰ از ۱۰
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_teacher")
            page.locator("input[type='password']").first.fill("TestPass1234!")
            page.locator("input[type='password']").first.press("Enter")
            page.locator("button[aria-label='افزودن']").wait_for()

            page.evaluate("(eid) => window.ExamSite.go('grading', { examId: eid })", exam_id)
            page.get_by_text("علی رضایی").first.wait_for()
            page.locator(".g-item", has_text="علی رضایی").first.click()
            page.wait_for_timeout(250)

            page.evaluate(
                """() => {
                    const gq = [...document.querySelectorAll('.g-q')][0];
                    const num = gq.querySelector('input[type=number]');
                    if (num) { num.value = '4'; num.dispatchEvent(new Event('input', { bubbles: true })); }
                    const txt = document.querySelector('.g-detail textarea') || gq.querySelector('input[type=text], textarea');
                    if (txt) {
                        txt.value = 'استدلال هندسی و کاربرد قضیهٔ فیثاغورس کاملاً دقیق و کامل است.';
                        txt.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )
            save_grade_btn = page.locator(".g-detail button").filter(has_text=re.compile(r"ذخیره|تأیید")).last
            save_grade_btn.scroll_into_view_if_needed()
            save_grade_btn.click()
            page.wait_for_function("() => document.body.innerText.includes('۱۰')", timeout=5000)

            # V340 — رفع مسابقهٔ زمانی: کلیک ذخیره و نشستِ نمره در سرور هم‌زمان نیستند
            deadline = time.monotonic() + 10
            updated_ans = next(iter(mock_state.answers.values()))
            while time.monotonic() < deadline and not updated_ans.get("graded"):
                time.sleep(0.1)
                updated_ans = next(iter(mock_state.answers.values()))
            assert updated_ans["graded"] is True
            assert updated_ans["total_grade"] == 10.0, f"نمرهٔ کل باید ۱۰ باشد اما {updated_ans['total_grade']} بود"
            page.evaluate("window.scrollTo(0, 0)")

            # V335.1 — تأیید نمایش کامل و بدون بریدگی وسط کلمهٔ تراشه‌های بازخورد تشویقی دبیر
            stamp_texts = page.locator(".g-detail .chip").all_inner_texts()
            for expected_stamp in (
                "آفرین قهرمان! خیلی خوب درخشیدی",
                "ستارهٔ طلایی برای دقت و تلاش عالی",
                "پیشرفت عالی نسبت به آزمون‌های قبل!",
            ):
                assert any(expected_stamp in t for t in stamp_texts), (
                    f"تراشهٔ بازخورد تشویقی نباید وسط کلمه بریده شود: {expected_stamp!r} در {stamp_texts!r}"
                )

            record_step(
                page,
                "08_teacher_grading_complete.png",
                "پنل تصحیح معلم — ثبت نمرهٔ تشریحی (۴/۴) و بازخورد اختصاصی؛ جمع نهایی ۱۰ از ۱۰",
                "تأیید نهایی نمرهٔ کل ۱۰ از ۱۰ و نمایش کامل تراشه‌های تشویقی بدون بریدگی وسط کلمه",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۸ب و ۸ج) V335.1 — بستن خودکار مودال‌ها با Esc/تغییر مسیر + ویرایشگر تک‌سؤال بانک سؤال هم‌تراز با اندروید
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.evaluate(
                "() => { window.__escRes = 'pending'; window.ExamSite.confirmDlg('آزمایش کلید Esc', 'با کلید Esc بسته می‌شود', 'تأیید').then(v => { window.__escRes = v; }); }"
            )
            page.locator(".modal-bg").first.wait_for(state="visible")
            page.keyboard.press("Escape")
            page.wait_for_function("() => document.querySelectorAll('.modal-bg').length === 0 && window.__escRes === false")

            page.evaluate(
                "() => { window.__navRes = 'pending'; window.ExamSite.confirmDlg('آزمایش تغییر مسیر', 'با جابه‌جایی صفحه بسته می‌شود', 'تأیید').then(v => { window.__navRes = v; }); }"
            )
            page.locator(".modal-bg").first.wait_for(state="visible")
            page.evaluate("() => window.ExamSite.go('bank')")
            page.wait_for_function(
                "() => document.querySelectorAll('.modal-bg').length === 0 && window.__navRes === false && document.body.innerText.includes('سؤال جدید در بانک')"
            )
            bank_empty_txt = page.locator("body").inner_text()
            assert "ذخیرهٔ سؤال جاری در بانک" not in bank_empty_txt, "متن کهنهٔ حالت خالی بانک نباید نمایش داده شود!"

            # افزودن دستهٔ «فصل اول» برای تست انتساب دسته در ویرایشگر تک‌سؤال بانک
            page.evaluate("() => window.ExamSite.rpcObj('native_bank_category_add_v1', { p_name: 'فصل اول' })")
            page.get_by_role("button", name=re.compile(r"سؤال جدید در بانک")).click()
            page.wait_for_selector(".b-settings", timeout=5000)
            page.get_by_text("فصل اول").first.wait_for(state="visible", timeout=5000)

            # در حالت تک‌سؤال بانک نباید دکمهٔ «+ افزودن سؤال»، «جمع بارم آزمون» یا دکمه‌های چندسؤالی نمایش داده شوند
            assert page.locator(".b-rail-btn.add").count() == 0, "در ویرایشگر تک‌سؤال بانک نباید دکمهٔ افزودن سؤال دوم نمایش داده شود!"
            assert page.locator(".b-rail-score").count() == 0, "در ویرایشگر تک‌سؤال بانک نباید جمع بارم آزمون نمایش داده شود!"
            assert page.locator(".b-qhead button[title='افزودن به بانک سؤال']").count() == 0, "در خود بانک نباید دکمهٔ افزودن به بانک نمایش داده شود!"

            fill_labeled_field(page.locator(".b-settings"), "درس", "فیزیک دهم")
            page.locator(".b-settings label.g-item").first.click()
            set_question_text(page, "قانون اول نیوتن (لَختی) را با یک مثال روزمره توضیح دهید.")
            page.evaluate(
                """() => {
                    const sel = document.querySelector("select[aria-label='نوع سؤال']");
                    sel.value = 'essay';
                    sel.dispatchEvent(new Event('change', { bubbles: true }));
                }"""
            )
            page.get_by_text("سؤال جدید بانک — تشریحی").first.wait_for(state="visible")
            assert "قانون اول نیوتن" in page.locator(".b-editor").inner_text(), "تغییر نوع سؤال در بانک نباید متن نوشته‌شده را پاک کند!"

            record_step(
                page,
                "08b_teacher_bank_builder.png",
                "ویرایشگر تک‌سؤال بانک سؤال V335.1 — پنل راست درس/دسته‌ها، انتخاب نوع سؤال و ریل اختصاصی",
                "نمایش ستون راست مشخصات سؤال بانک (درس و دسته‌ها)، انتخاب مستقیم نوع سؤال (تشریحی) و حذف دکمه‌های گمراه‌کنندهٔ چندسؤالی",
                "1366×900",
                t0,
            )

            t0 = time.monotonic()
            page.locator(".b-rail-btn.ok").click()
            card_loc = page.locator(".bank-card").first
            card_loc.wait_for(state="visible", timeout=5000)
            card_loc.get_by_text("فیزیک دهم").wait_for(state="visible", timeout=5000)
            card_loc.get_by_text("فصل اول").wait_for(state="visible", timeout=5000)

            record_step(
                page,
                "08c_teacher_bank_saved.png",
                "بانک سؤال پس از ذخیرهٔ تک‌سؤال تشریحی — نمایش کارت سؤال با درس و برچسب دسته",
                "ثبت موفق سؤال تشریحی در بانک سؤال همراه درس «فیزیک دهم» و دستهٔ «فصل اول» و بازگشت خودکار به فهرست بانک",
                "1366×900",
                t0,
            )

            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۹) کارنامهٔ تفصیلی دانش‌آموز و نمایش نمرهٔ ۱۰ از ۱۰ و بازخورد دبیر
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_student")
            page.locator("input[type='password']").first.fill("StudentPass123!")
            page.locator("input[type='password']").first.press("Enter")
            page.wait_for_function("() => !document.querySelector('input[type=password]')")

            page.evaluate("() => window.ExamSite.go('grades')")
            page.get_by_text(exam_title).first.wait_for()
            detail_btn = page.locator("button").filter(has_text=re.compile(r"پاسخ‌نامه|جزئیات|مشاهده")).first
            if detail_btn.count() > 0:
                detail_btn.click()
            else:
                page.get_by_text(exam_title).first.click()
            fb_loc = page.get_by_text("استدلال هندسی و کاربرد قضیهٔ فیثاغورس کاملاً دقیق و کامل است.").first
            fb_loc.wait_for(state="attached")
            fb_loc.scroll_into_view_if_needed()
            fb_loc.wait_for(state="visible")

            record_step(
                page,
                "09_student_report_card.png",
                "کارنامهٔ نهایی دانش‌آموز — نمایش نمرهٔ ۱۰ از ۱۰ و بازخورد تشریحی دبیر",
                "تأیید نمایش پاسخ‌نامهٔ کامل، نمرهٔ هر سؤال و بازخورد دبیر در کارنامهٔ دانش‌آموز",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۹ب) V339 — انتخاب قالب کارنامه: ۱۰ قالب → پیش‌نمایش A4 → چاپ
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.evaluate("() => window.ExamSite.go('grades')")
            page.get_by_role("button", name="ذخیرهٔ کارنامه به‌صورت PDF").first.click()
            rc_frame = page.frame_locator("iframe[title='report-card-templates']")
            rc_frame.locator(".rc-card").first.wait_for(timeout=60000)
            card_count = rc_frame.locator(".rc-card").count()
            assert card_count == 10, f"باید ۱۰ قالب کارنامه پیشنهاد شود، یافت شد: {card_count}"
            record_step(
                page,
                "09b_report_card_templates.png",
                "انتخاب قالب کارنامه — هر ۱۰ قالب برای کارنامهٔ دانش‌آموز",
                "تأیید نمایش انتخابگر ۱۰ قالب (عمومی و فرم رسمی) پیش از چاپ کارنامه",
                "1366×900",
                t0,
            )

            t0 = time.monotonic()
            rc_frame.locator(".rc-card").first.click()
            rc_frame.locator(".page").first.wait_for(timeout=30000)
            rc_metrics = page.evaluate(
                """() => {
                    const f = document.querySelector("iframe[title='report-card-templates']");
                    const d = f.contentDocument;
                    const pages = [...d.querySelectorAll('.page')];
                    return {count: pages.length, max: Math.max(...pages.map(p => Math.ceil(p.getBoundingClientRect().height))),
                            text: pages.map(p => p.innerText).join(' ')};
                }"""
            )
            assert rc_metrics["count"] >= 1, "پیش‌نمایش کارنامه باید دست‌کم یک برگهٔ A4 داشته باشد"
            assert rc_metrics["max"] <= 1123, f"برگهٔ کارنامه از A4 بیرون زده است: {rc_metrics['max']}"
            # دادهٔ واقعی باید بنشیند: نمرهٔ ۱۰ از ۱۰ این دانش‌آموز = ۱۰۰٪ (نه «غایب»/صفر)
            assert "۱۰۰" in rc_metrics["text"], "درصد نمرهٔ دانش‌آموز در قالب کارنامه ننشسته است"
            assert "غایب" not in rc_metrics["text"], "نمرهٔ تصحیح‌شده به‌اشتباه «غایب» رندر شده است"
            # پیش‌نمایش تمام‌صفحه است و متن صفحهٔ میزبان زیر آن پنهان می‌شود؛
            # به‌جای بررسی چیدمان میزبان، خودِ برگهٔ داخل iframe سنجیده می‌شود.
            rc_print_btn = rc_frame.get_by_role("button", name=re.compile("چاپ"))
            assert rc_print_btn.count() == 1, "دکمهٔ چاپ/PDF باید در نوار پیش‌نمایش باشد"
            page.screenshot(path=str(out_dir / "09c_report_card_preview.png"), full_page=False, animations="disabled")
            steps.append({
                "title": "پیش‌نمایش A4 کارنامه با قالب انتخاب‌شده — آمادهٔ چاپ",
                "detail": "تأیید رندر قالب روی برگهٔ A4 بدون سرریز (%d برگه، بیشترین ارتفاع %dpx) و نمایش دکمهٔ چاپ/PDF" % (rc_metrics["count"], rc_metrics["max"]),
                "viewport": "1366×900",
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": "09c_report_card_preview.png",
            })
            print("  [PASS] 09c_report_card_preview.png — پیش‌نمایش A4 کارنامه (%dms)" % steps[-1]["duration_ms"], flush=True)
            page.evaluate("() => window.ExamReportCard.close()")

            # --------------------------------------------------------
            # ۹د) V339 — کارنامهٔ کلاس: فرم رسمی ۱۰۱ + مرحلهٔ تناظر آزمون‌ها
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.evaluate(
                """() => {
                    const cols = [], rows = [], roles = {};
                    const subjects = ['ریاضی', 'فیزیک', 'شیمی', 'فارسی'];
                    subjects.forEach((s, i) => {
                        cols.push({key: 'a' + i, title: s + ' — مستمر', subject: s, max: 20});
                        cols.push({key: 'b' + i, title: s + ' — پایانی', subject: s, max: 20});
                        roles['a' + i] = 'mostamar'; roles['b' + i] = 'payani';
                    });
                    for (let i = 0; i < 18; i++) {
                        const vals = {};
                        cols.forEach((c, j) => { vals[c.key] = 10 + ((i * 7 + j * 3) % 11); });
                        rows.push({name: 'دانش‌آموز ' + (i + 1), vals: vals});
                    }
                    window.ExamReportCard.open({
                        scope: 'class', className: 'دهم ۱۰۱', grade: 'دهم',
                        school: {name: 'دبیرستان نمونهٔ دولتی شهید بهشتی', province: 'تهران', district: '۳'},
                        year: 'سال تحصیلی ۱۴۰۵–۱۴۰۶', date: '۱۴۰۵/۰۸/۱۴',
                        cols: cols, rows: rows, roles: roles, structure: 'combined'
                    });
                }"""
            )
            rc_frame.locator(".rc-card").first.wait_for(timeout=30000)
            rc_frame.locator(".rc-card").nth(5).click()
            rc_frame.locator(".rc-map select").first.wait_for(timeout=15000)
            role_count = rc_frame.locator(".rc-map select").count()
            assert role_count == 8, f"مرحلهٔ تناظر باید برای هر ۸ آزمون یک انتخابگر داشته باشد، یافت شد: {role_count}"
            rc_frame.get_by_role("button", name="پیش‌نمایش").click()
            rc_frame.locator(".page").first.wait_for(timeout=30000)
            cls_metrics = page.evaluate(
                """() => {
                    const d = document.querySelector("iframe[title='report-card-templates']").contentDocument;
                    const pages = [...d.querySelectorAll('.page')];
                    return {count: pages.length, max: Math.max(...pages.map(p => Math.ceil(p.getBoundingClientRect().height))),
                            text: pages.map(p => p.innerText).join(' '),
                            matrix: d.querySelectorAll('.agg-table.matrix').length,
                            subjects: d.querySelectorAll('.agg-table').length};
                }"""
            )
            assert cls_metrics["max"] <= 1123, f"برگهٔ کارنامهٔ کلاس از A4 بیرون زده است: {cls_metrics['max']}"
            assert cls_metrics["matrix"] >= 1, "ماتریس نمرات کلاس باید رندر شود"
            assert "نمرهٔ نوبت" in cls_metrics["text"], "ستون نمرهٔ نوبت فرم ۱۰۱ باید در کارنامهٔ کلاس باشد"
            page.screenshot(path=str(out_dir / "09d_report_card_class_form.png"), full_page=False, animations="disabled")
            steps.append({
                "title": "کارنامهٔ کلاس با فرم رسمی ۱۰۱ — تناظر آزمون‌ها و ماتریس نمرات",
                "detail": "تأیید مرحلهٔ انتخاب نقش هر آزمون (مستمر/پایانی)، محاسبهٔ نمرهٔ نوبت و رندر %d برگهٔ A4 بدون سرریز" % cls_metrics["count"],
                "viewport": "1366×900",
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": "09d_report_card_class_form.png",
            })
            print("  [PASS] 09d_report_card_class_form.png — کارنامهٔ کلاس/فرم ۱۰۱ (%dms)" % steps[-1]["duration_ms"], flush=True)
            page.evaluate("() => window.ExamReportCard.close()")

            # --------------------------------------------------------
            # ۹ه) V339.2 — پوستهٔ اپ: هدر ثابت سه‌ردیفه، برگه هم‌عرض گوشی، زوم فقط روی برگه
            # --------------------------------------------------------
            t0 = time.monotonic()
            # صفحهٔ گوشی از ابتدا در اندازهٔ خودش ساخته می‌شود؛ تغییر اندازهٔ صفحهٔ
            # دسکتاپ در کروم بی‌سر، محتوای iframe را از رنگ‌اندازی می‌اندارد.
            rc_app_ctx = browser.new_context(
                viewport={"width": 390, "height": 844},
                locale="fa-IR",
                is_mobile=True,
                has_touch=True,
                service_workers="block",
            )
            rc_page = rc_app_ctx.new_page()
            rc_page.set_default_timeout(20000)
            rc_page.on("pageerror", lambda err: page_errors.append(str(err)))
            rc_page.goto(web_url + "/", wait_until="domcontentloaded")
            rc_page.wait_for_function("() => window.ExamReportCard && window.ExamSite")
            rc_page.evaluate(
                """() => {
                    const cols = [], rows = [], roles = {};
                    ['ریاضی', 'فیزیک', 'شیمی', 'فارسی'].forEach((s, i) => {
                        cols.push({key: 'a' + i, title: s + ' — مستمر', subject: s, max: 20});
                        cols.push({key: 'b' + i, title: s + ' — پایانی', subject: s, max: 20});
                        roles['a' + i] = 'mostamar'; roles['b' + i] = 'payani';
                    });
                    for (let i = 0; i < 18; i++) {
                        const vals = {};
                        cols.forEach((c, j) => { vals[c.key] = 10 + ((i * 7 + j * 3) % 11); });
                        rows.push({name: 'دانش‌آموز ' + (i + 1), vals: vals});
                    }
                    const p = {scope: 'class', className: 'دهم ۱۰۱', grade: 'دهم',
                        school: {name: 'دبیرستان نمونهٔ دولتی شهید بهشتی', province: 'تهران', district: '۳'},
                        year: 'سال تحصیلی ۱۴۰۵–۱۴۰۶', date: '۱۴۰۵/۰۸/۱۴',
                        cols: cols, rows: rows, roles: roles, structure: 'combined'};
                    window.__rcAppPayload = p;
                    window.ExamReportCard.open(p);
                }"""
            )
            rc_page_f = rc_page.frame_locator("iframe[title='report-card-templates']")
            rc_page_f.locator(".rc-card").first.wait_for(timeout=60000)
            # میزبان اندروید پوستهٔ اپ را درخواست می‌کند؛ سایت خودش این گزینه را نمی‌فرستد.
            rc_page.evaluate(
                """() => {
                    const w = document.querySelector("iframe[title='report-card-templates']").contentWindow;
                    w.ReportCard.open(window.__rcAppPayload, {skin: 'app'});
                }"""
            )
            rc_page_f.locator(".rc-card").nth(5).click()
            rc_page_f.locator(".rc-map select").first.wait_for(timeout=15000)
            rc_page_f.get_by_role("button", name="پیش‌نمایش").click()
            rc_page_f.locator(".page").first.wait_for(timeout=30000)
            rc_page.wait_for_timeout(250)
            rc_app_js = """() => {
                const f = document.querySelector("iframe[title='report-card-templates']");
                const d = f.contentDocument, w = f.contentWindow;
                const bar = d.getElementById('rc-bar'), root = d.getElementById('rc-root');
                const btns = [...bar.querySelectorAll('.rc-bar-row .rc-btn')];
                const title = bar.querySelector('.rc-bar-title'), slot = bar.querySelector('.rc-bar-struct');
                const sizer = root.querySelector('.rc-sizer');
                const pages = [...root.querySelectorAll('.page')];
                const r = e => e.getBoundingClientRect();
                return {vw: w.innerWidth, overflowX: d.documentElement.scrollWidth - w.innerWidth,
                    pos: w.getComputedStyle(bar).position, barTop: r(bar).top, barH: r(bar).height, barW: r(bar).width,
                    n: btns.length, tops: btns.map(b => Math.round(r(b).top)),
                    inside: btns.every(b => r(b).left >= 0 && r(b).right <= w.innerWidth),
                    titleTop: r(title).top, btnBottom: Math.max(...btns.map(b => r(b).bottom)),
                    slotTop: r(slot).top, titleBottom: r(title).bottom, slotN: slot.querySelectorAll('button').length,
                    sizerW: Math.ceil(r(sizer).width), rootW: root.clientWidth,
                    pageH: Math.max(...pages.map(p => p.offsetHeight)),
                    pageW: Math.round(r(pages[0]).width), zoom: w.ReportCard.state.zoom, skin: w.ReportCard.state.skin};
            }"""
            app_m = rc_page.evaluate(rc_app_js)
            assert app_m["skin"] == "app", "پوستهٔ اپ فعال نشد"
            assert app_m["pos"] == "fixed", "هدر پیش‌نمایش اپ باید ثابت باشد"
            assert app_m["n"] == 3, f"هدر اپ باید سه دکمه داشته باشد، یافت شد: {app_m['n']}"
            assert len(set(app_m["tops"])) == 1, f"دکمه‌های هدر باید در یک ردیف باشند: {app_m['tops']}"
            assert app_m["inside"], "دکمه‌ای از هدر بیرون کادر گوشی است"
            assert app_m["overflowX"] <= 0, f"پیش‌نمایش اپ اسکرول افقی دارد: {app_m['overflowX']}"
            assert abs(app_m["barW"] - app_m["vw"]) < 1, "هدر هم‌عرض صفحهٔ گوشی نیست"
            assert app_m["titleTop"] >= app_m["btnBottom"] - 1, "نام نوع کارنامه باید زیر دکمه‌ها باشد"
            assert app_m["slotN"] == 3, "ساختار جدول باید در هدر اپ باشد"
            assert app_m["slotTop"] >= app_m["titleBottom"] - 1, "ساختار جدول باید زیر نام کارنامه باشد"
            assert app_m["sizerW"] <= app_m["rootW"], "برگهٔ کارنامه از عرض گوشی بیرون زده است"
            assert app_m["pageW"] <= app_m["vw"], "برگهٔ کوچک‌نمایی‌شده از صفحه بیرون زده است"
            assert app_m["pageH"] <= 1124, f"برگه از A4 بلندتر است: {app_m['pageH']}"
            rc_page.screenshot(path=str(out_dir / "09e_report_card_app_chrome.png"), full_page=False, animations="disabled")
            # زوم دو‌انگشتی: فقط برگه بزرگ شود، هدر تکان نخورد
            rc_page.evaluate(
                """() => {
                    const d = document.querySelector("iframe[title='report-card-templates']").contentDocument;
                    const root = d.getElementById('rc-root');
                    const fire = (type, pts) => {
                        const ev = new Event(type, {bubbles: true, cancelable: true});
                        Object.defineProperty(ev, 'touches', {value: pts.map(p => ({clientX: p[0], clientY: p[1]}))});
                        root.dispatchEvent(ev);
                    };
                    fire('touchstart', [[150, 500], [250, 500]]);
                    fire('touchmove', [[100, 500], [300, 500]]);
                    fire('touchend', []);
                }"""
            )
            rc_page.wait_for_timeout(200)
            app_z = rc_page.evaluate(rc_app_js)
            assert app_z["zoom"] > app_m["zoom"] + 0.05, "زوم دو‌انگشتی روی برگه کار نکرد"
            assert app_z["pageW"] > app_m["pageW"], "برگه با زوم دو‌انگشتی بزرگ نشد"
            assert abs(app_z["barH"] - app_m["barH"]) < 1 and abs(app_z["barTop"] - app_m["barTop"]) < 1, "هدر با زوم بزرگ/جابه‌جا شد"
            assert app_z["tops"] == app_m["tops"], "ردیف دکمه‌ها با زوم تغییر کرد"
            assert app_z["overflowX"] <= 0, "بعد از زوم، سند اسکرول افقی گرفت"
            rc_page.screenshot(path=str(out_dir / "09f_report_card_app_zoom.png"), full_page=False, animations="disabled")
            # چاپ باید زوم را خنثی کند
            rc_page.emulate_media(media="print")
            app_pr = rc_page.evaluate(
                """() => {
                    const f = document.querySelector("iframe[title='report-card-templates']");
                    const d = f.contentDocument, w = f.contentWindow;
                    return {transform: w.getComputedStyle(d.querySelector('.rc-scale')).transform,
                        bar: w.getComputedStyle(d.getElementById('rc-bar')).display,
                        pageW: Math.round(d.querySelector('.page').getBoundingClientRect().width)};
                }"""
            )
            rc_page.emulate_media(media="screen")
            assert app_pr["transform"] == "none", f"زوم در چاپ خنثی نشد: {app_pr['transform']}"
            assert app_pr["bar"] == "none", "هدر در چاپ پنهان نشد"
            assert abs(app_pr["pageW"] - 794) <= 2, f"عرض برگه در چاپ A4 نیست: {app_pr['pageW']}"
            steps.append({
                "title": "پوستهٔ اپ کارنامه در گوشی — هدر ثابت سه‌ردیفه و برگه هم‌عرض صفحه",
                "detail": "سه دکمه در یک ردیف، نام قالب زیر آن و ساختار جدول زیر نام؛ برگه هم‌عرض گوشی (%dpx از %dpx)، زوم دو‌انگشتی فقط روی برگه و چاپ بدون زوم روی A4" % (app_m["sizerW"], app_m["vw"]),
                "viewport": "390×844",
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": "09e_report_card_app_chrome.png",
            })
            steps.append({
                "title": "زوم دو‌انگشتی کارنامه در اپ — هدر ثابت و خوانا می‌ماند",
                "detail": "برگه از %dpx به %dpx بزرگ شد ولی ارتفاع و جای هدر و ردیف دکمه‌ها تکان نخورد؛ سند اسکرول افقی نگرفت" % (app_m["pageW"], app_z["pageW"]),
                "viewport": "390×844",
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": "09f_report_card_app_zoom.png",
            })
            print("  [PASS] 09e_report_card_app_chrome.png — پوستهٔ اپ کارنامه در گوشی (%dms)" % steps[-1]["duration_ms"], flush=True)
            print("  [PASS] 09f_report_card_app_zoom.png — زوم فقط روی برگه، هدر ثابت", flush=True)
            rc_page.evaluate("() => window.ExamReportCard.close()")
            rc_app_ctx.close()

            # --------------------------------------------------------
            # ۹ج) V340 — کارنامهٔ مدیر: دفترچهٔ مدرسه با ۱۰ قالب
            # --------------------------------------------------------
            mgr_ctx = browser.new_context(viewport={"width": 1366, "height": 900})
            mgr_page = mgr_ctx.new_page()
            mgr_page.on("pageerror", lambda err: page_errors.append(str(err)))
            mgr_page.goto(web_url + "/", wait_until="domcontentloaded")
            mgr_page.locator("input[type='text']").first.fill("uitest_manager")
            mgr_page.locator("input[type='password']").first.fill("ManagerPass123!")
            mgr_page.locator("input[type='password']").first.press("Enter")
            mgr_page.wait_for_function("() => !document.querySelector('input[type=password]')", timeout=60000)

            mgr_page.evaluate("() => window.ExamSite.go('dashboard', {report: true})")

            t0 = time.monotonic()
            booklet_btn = mgr_page.get_by_role("button", name=re.compile("کارنامهٔ مدرسه \\(PDF\\)")).first
            booklet_btn.wait_for(timeout=60000)
            booklet_btn.click()
            mgr_frame = mgr_page.frame_locator("iframe[title='report-card-templates']")
            mgr_frame.locator(".rc-card").first.wait_for(timeout=90000)
            mgr_cards = mgr_frame.locator(".rc-card").count()
            assert mgr_cards == 10, f"دفترچهٔ مدرسه هم باید هر ۱۰ قالب را پیشنهاد دهد، یافت شد: {mgr_cards}"
            record_step(
                mgr_page,
                "09g_manager_school_booklet.png",
                "کارنامهٔ مدیر — دفترچهٔ مدرسه با هر ۱۰ قالب",
                "تأیید دکمهٔ «کارنامهٔ مدرسه (PDF)» در پنل مدیر و نمایش انتخابگر ۱۰ قالب برای سطح مدرسه",
                "1366×900",
                t0,
            )

            t0 = time.monotonic()
            mgr_frame.locator(".rc-card").first.click()
            mgr_frame.locator(".page").first.wait_for(timeout=90000)
            booklet = mgr_page.evaluate(
                """() => {
                    const d = document.querySelector("iframe[title='report-card-templates']").contentDocument;
                    const pages = [...d.querySelectorAll('.page')];
                    return {count: pages.length,
                            max: Math.max(...pages.map(p => Math.ceil(p.getBoundingClientRect().height))),
                            text: pages.map(p => p.innerText).join(' ')};
                }"""
            )
            assert booklet["count"] >= 5, f"دفترچه باید نمای کلی + خلاصه + کارنامهٔ هر کلاس باشد: {booklet['count']}"
            assert booklet["max"] <= 1123, f"برگهٔ دفترچه از A4 بیرون زده است: {booklet['max']}"
            assert "کارنامهٔ مدرسه" in booklet["text"], "سربرگ دفترچهٔ مدرسه رندر نشده است"
            assert "تعداد کلاس" in booklet["text"], "خلاصهٔ مقایسهٔ کلاس‌ها در دفترچه نیست"
            assert "سال تحصیلی" in booklet["text"], "سال تحصیلی در سربرگ دفترچه ننشسته است"
            assert "تاریخ صدور" in booklet["text"], "تاریخ صدور در سربرگ دفترچه ننشسته است"
            assert "دهم ریاضی ۲" in booklet["text"], "کارنامهٔ کامل کلاس دوم در دفترچه نیست"
            assert "سارا محمدی" in booklet["text"], "نمرهٔ دانش‌آموزان در کارنامهٔ کلاس‌های دفترچه ننشسته است"
            mgr_print = mgr_frame.get_by_role("button", name=re.compile("چاپ"))
            assert mgr_print.count() == 1, "دکمهٔ چاپ/PDF باید در نوار پیش‌نمایش دفترچه باشد"
            mgr_page.screenshot(path=str(out_dir / "09h_manager_booklet_preview.png"), full_page=False, animations="disabled")
            steps.append({
                "title": "پیش‌نمایش دفترچهٔ مدرسه — نمای کلی، خلاصه و کارنامهٔ کامل هر کلاس",
                "detail": "تأیید رندر دفترچه روی A4 بدون سرریز (%d برگه، بیشترین ارتفاع %dpx) با کارنامهٔ تک‌تک کلاس‌ها" % (booklet["count"], booklet["max"]),
                "viewport": "1366×900",
                "duration_ms": int((time.monotonic() - t0) * 1000),
                "passed": True,
                "screenshot": "09h_manager_booklet_preview.png",
            })
            print("  [PASS] 09h_manager_booklet_preview.png — دفترچهٔ مدرسه (%dms)" % steps[-1]["duration_ms"], flush=True)
            mgr_page.evaluate("() => window.ExamReportCard.close()")

            # --------------------------------------------------------
            # ۹ط) V340 — کارنامهٔ انفرادی دانش‌آموز از پنل مدیر
            # --------------------------------------------------------
            t0 = time.monotonic()
            one = mgr_page.evaluate(
                """async () => {
                    const d = await window.ExamSite.rpcObj('native_manager_class_report_v1',
                        {p_class: '44444444-4444-4444-8444-444444444444'});
                    const P = window.ExamReportCard;
                    P.open(P.fromManagerStudent(d, 'سارا محمدی'));
                    return true;
                }"""
            )
            assert one, "ساخت دادهٔ کارنامهٔ انفرادی مدیر ناموفق بود"
            mgr_frame.locator(".rc-card").first.wait_for(timeout=60000)
            assert mgr_frame.locator(".rc-card").count() == 10, "کارنامهٔ انفرادی مدیر هم باید ۱۰ قالب داشته باشد"
            mgr_frame.locator(".rc-card").nth(3).click()
            mgr_frame.locator(".page").first.wait_for(timeout=60000)
            single = mgr_page.evaluate(
                """() => {
                    const d = document.querySelector("iframe[title='report-card-templates']").contentDocument;
                    const pages = [...d.querySelectorAll('.page')];
                    return {count: pages.length,
                            max: Math.max(...pages.map(p => Math.ceil(p.getBoundingClientRect().height))),
                            text: pages.map(p => p.innerText).join(' ')};
                }"""
            )
            assert "سارا محمدی" in single["text"], "نام دانش‌آموز در کارنامهٔ انفرادی مدیر ننشسته است"
            assert single["max"] <= 1123, f"کارنامهٔ انفرادی مدیر از A4 بیرون زده است: {single['max']}"
            record_step(
                mgr_page,
                "09i_manager_student_card.png",
                "کارنامهٔ انفرادی دانش‌آموز از پنل مدیر — همان قابلیت معلم",
                "تأیید ساخت کارنامهٔ یک دانش‌آموز با دادهٔ کلاس مدیر، ۱۰ قالب و پیش‌نمایش A4 بدون سرریز",
                "1366×900",
                t0,
            )
            mgr_page.evaluate("() => window.ExamReportCard.close()")
            mgr_ctx.close()

            desktop_ctx.close()
            browser.close()

        after_hash = hashlib.sha256((site_dir / "index.html").read_bytes()).hexdigest()
        assert before_hash == after_hash, "فایل‌های site_out روی دیسک نباید حین تست دست‌کاری شوند!"
        assert not page_errors, f"خطاهای جاوااسکریپت در مرورگر رخ داد: {page_errors}"
        assert not asset_failures, f"خطای بارگذاری دارایی‌های سایت: {asset_failures}"

        meta = {
            "exam_id": exam_id,
            "exam_code": exam_code,
            "page_errors": page_errors,
            "asset_failures": asset_failures,
            "rpc_calls": len(mock_state.rpc_log),
        }
        write_visual_reports(out_dir, steps, meta)
        print(f"WEB_UI_E2E_ALL_GREEN steps={len(steps)} code={exam_code} rpc_calls={len(mock_state.rpc_log)}", flush=True)
        return 0
    except Exception as exc:
        print("CI_FAILURE_PAGE_ERRORS:", page_errors, flush=True)
        print("CI_FAILURE_ASSET_FAILURES:", asset_failures, flush=True)
        print("CI_FAILURE_RPC_TAIL:", json.dumps(mock_state.rpc_log[-20:], ensure_ascii=False), flush=True)
        raise exc
    finally:
        web_srv.shutdown()
        mock_srv.shutdown()
        if temp_site_ctx is not None:
            temp_site_ctx.cleanup()


def main() -> None:
    parser = argparse.ArgumentParser(description="Automated Chromium Visual & Functional E2E Suite")
    parser.add_argument("--site-dir", default=None, help="مسیر site_out ساخته‌شده (اختیاری)")
    parser.add_argument("--out-dir", default="/tmp/web-ui-report", help="مسیر ذخیرهٔ اسکرین‌شات‌ها و گزارش")
    args = parser.parse_args()
    sys.exit(run_suite(args.site_dir, Path(args.out_dir)))


if __name__ == "__main__":
    main()
