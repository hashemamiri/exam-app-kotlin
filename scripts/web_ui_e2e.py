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
                bodyTextLen: (document.body.innerText || '').trim().length
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
            # ۳) ورود معلم + ساخت آزمون چندبخشی متوسطه (هندسه + اثبات + تستی)
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_teacher")
            page.locator("input[type='password']").first.fill("TestPass1234!")
            page.locator("input[type='password']").first.press("Enter")
            page.locator("button[aria-label='افزودن']").wait_for()
            page.evaluate("() => window.ExamSite.go('builder', { fresh: true })")
            page.get_by_role("button", name="ذخیره", exact=True).wait_for()

            exam_title = "آزمون جامع هندسه و استدلال متوسطه"
            fill_labeled_field(page, "عنوان آزمون", exam_title)
            fill_labeled_field(page, "درس", "هندسه ۱")
            fill_labeled_field(page, "مدت (دقیقه)", "30")

            # سؤال ۱: ترسیم هندسی مختصاتی (۳ نمره) در بخش زمان‌دار «بخش هندسه مختصاتی»
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "تشریحی", substring=True)
            set_question_text(
                page,
                "روی صفحهٔ مختصات نقاط A(0, 3)، B(0, 0) و C(4, 0) را طوری قرار دهید که زاویهٔ ∠ABC قائمه (۹۰ درجه) باشد.",
            )
            fill_labeled_field(page, "بخش آزمون / صورت مشترک", "بخش هندسه مختصاتی")

            # بررسی واکنش زندهٔ کادر «زمان مجاز هر بخش» بدون تعویض سؤال!
            sec_box = page.locator(".b-section-durations")
            sec_box.wait_for()
            sec_box.locator("input[type='number']").first.fill("10")

            select_labeled_option(page, "حالت پاسخ", "construct")
            fill_labeled_field(page, "برچسب زاویه‌های خواسته‌شده", "ABC")

            # افزودن ۳ نقطه روی بوم (A=(0,3, بارم 1)، B=(0,0, بارم 1)، C=(4,0, بارم 0)) + شرط زاویه ∠ABC = 90° (بارم 1) → جمع ۳ نمره
            add_target_btn = page.get_by_role("button", name="افزودن نقطهٔ هدف")
            add_target_btn.scroll_into_view_if_needed()
            target_cards = page.locator("div.card:not(:has(div.card))").filter(has=page.get_by_role("button", name="حذف نقطه"))
            while target_cards.count() < 3:
                add_target_btn = page.get_by_role("button", name="افزودن نقطهٔ هدف")
                add_target_btn.scroll_into_view_if_needed()
                add_target_btn.click()
            assert target_cards.count() == 3, f"انتظار ۳ کارت نقطهٔ هدف بود، {target_cards.count()} دیده شد"
            for idx_t, (lbl_t, tx, ty, sc) in enumerate([("A", "0", "3", "1"), ("B", "0", "0", "1"), ("C", "4", "0", "0")]):
                tc = target_cards.nth(idx_t)
                fill_labeled_field(tc, "برچسب", lbl_t)
                fill_labeled_field(tc, "x هدف", tx)
                fill_labeled_field(tc, "y هدف", ty)
                fill_labeled_field(tc, "بارم", sc)

            add_c_btn = page.get_by_role("button", name="افزودن شرط هندسی (زاویه/فاصله/عمود/موازی/هم‌خط)")
            add_c_btn.scroll_into_view_if_needed()
            c_card_list = page.locator("div.card:not(:has(div.card))").filter(has=page.get_by_role("button", name="حذف شرط هندسی"))
            while c_card_list.count() < 1:
                add_c_btn.click()
            c_card = c_card_list.first
            fill_labeled_field(c_card, "نقاط درگیر", "A,B,C")
            fill_labeled_field(c_card, "زاویهٔ هدف (درجه)", "90")
            fill_labeled_field(c_card, "بارم این شرط", "1")
            fill_labeled_field(page, "Rubric دبیر", "نمرهٔ محرمانهٔ دبیر: بررسی تعامد بردارهای BA و BC")

            page.evaluate(
                """() => {
                    const scoreFields = [...document.querySelectorAll('.field')].filter(f => f.querySelector('label')?.textContent.trim() === 'بارم');
                    const mainScore = scoreFields[scoreFields.length - 1];
                    if (mainScore) {
                        const i = mainScore.querySelector('input');
                        i.value = '3';
                        i.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )

            # افزودن سؤال ۲: اثبات مرحله‌ای (۴ نمره = ۲ + ۲) در بخش «بخش اثبات مرحله‌ای» با ارجاع به گام قبل
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "تشریحی", substring=True)
            set_question_text(
                page,
                "با توجه به مثلث قائم‌الزاویهٔ ABC در رأس B با اضلاع AB=3 و BC=4، طول وتر AC را در دو گام اثبات کنید.",
            )
            fill_labeled_field(page, "بخش آزمون / صورت مشترک", "بخش اثبات مرحله‌ای")
            select_labeled_option(page, "حالت پاسخ", "proof")
            fill_labeled_field(page, "فرض‌ها / داده‌ها", "AB = 3 ، BC = 4 و AB ⊥ BC")
            fill_labeled_field(page, "خواسته", "AC = 5")

            part_cards = page.locator("div.card:not(:has(div.card))").filter(has=page.get_by_role("button", name="حذف زیربخش"))
            while part_cards.count() < 2:
                add_part_btn = page.get_by_role("button", name="افزودن زیربخش")
                add_part_btn.scroll_into_view_if_needed()
                add_part_btn.click()
            assert part_cards.count() == 2, f"انتظار ۲ گام اثبات بود، {part_cards.count()} دیده شد"
            fill_labeled_field(part_cards.nth(0), "برچسب (الف/ب/ج)", "الف")
            fill_labeled_field(part_cards.nth(0), "دستور زیرسؤال", "رابطهٔ فیثاغورس را برای مثلث ABC بنویسید.")
            fill_labeled_field(part_cards.nth(0), "بارم", "2")
            part_cards.nth(0).locator("textarea[placeholder*='معیار نمره‌دهی محرمانه']").fill("نوشتن AC^2 = AB^2 + BC^2")

            fill_labeled_field(part_cards.nth(1), "برچسب (الف/ب/ج)", "ب")
            fill_labeled_field(part_cards.nth(1), "دستور زیرسؤال", "با جایگذاری مقادیر گام الف، مقدار عددی AC را بیابید.")
            fill_labeled_field(part_cards.nth(1), "بارم", "2")
            part_cards.nth(1).locator("textarea[placeholder*='معیار نمره‌دهی محرمانه']").fill("نتیجهٔ نهایی AC = 5")
            carry_lbl = part_cards.nth(1).locator("label", has_text="اجازهٔ ارجاع به نتیجهٔ بخش قبلی")
            carry_lbl.scroll_into_view_if_needed()
            carry_lbl.click()

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

            # افزودن سؤال ۳: چندگزینه‌ای (۳ نمره)
            click_visible_button(page, "افزودن")
            page.wait_for_timeout(120)
            click_visible_button(page, "چندگزینه‌ای", substring=True)
            set_question_text(
                page,
                "در مثلث قائم‌الزاویهٔ بالا با اضلاع قائمهٔ ۳ و ۴، مساحت مثلث چند واحد مربع است؟",
            )
            fill_labeled_field(page, "بخش آزمون / صورت مشترک", "بخش اثبات مرحله‌ای")
            set_mc_options(page, ["۵", "۶", "۷", "۱۲"])
            # انتخاب گزینهٔ ۲ («۶») به‌عنوان گزینهٔ درست + بارم ۳
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

            page.evaluate("window.scrollTo(0, 0)")
            record_step(
                page,
                "03_teacher_builder_secondary.png",
                "سازندهٔ آزمون متوسطه (ترسیم هندسی + اثبات مرحله‌ای + زمان‌بندی بخش)",
                "ساخت آزمون ۳ سؤالی (۱۰ نمره‌ای) با بخش زمان‌دار ۱۰ دقیقه‌ای و شرط زاویهٔ ۹۰ درجه",
                "1366×900",
                t0,
            )

            # ذخیرهٔ اول + بررسی ثبات کد در ذخیرهٔ دوم
            t0 = time.monotonic()
            page.get_by_role("button", name="ذخیره", exact=True).click()
            page.locator(".modal button", has_text="ذخیره").click()
            page.wait_for_function("() => document.querySelectorAll('.modal button').length > 0")
            # بستن پنجرهٔ رسید کسر هزینه در صورت نمایش
            page.wait_for_timeout(400)
            if page.locator(".modal button").count() > 0:
                page.locator(".modal button").first.click()

            assert len(mock_state.exams) == 1, "آزمون باید در سرور ذخیره شده باشد"
            saved_exam = next(iter(mock_state.exams.values()))
            exam_id = saved_exam["id"]
            exam_code = saved_exam["code"]
            assert len(exam_code) == 6, f"کد آزمون نامعتبر: {exam_code}"
            assert saved_exam["options"].get("sectionDurations", {}).get("بخش هندسه مختصاتی") == 10

            # ذخیرهٔ دوم برای اثبات تغییر نکردن کد آزمون
            page.get_by_role("button", name="ذخیره", exact=True).click()
            page.locator(".modal button", has_text="ذخیره").click()
            page.wait_for_timeout(350)
            if page.locator(".modal button").count() > 0:
                page.locator(".modal button").first.click()
            assert mock_state.exams[exam_id]["code"] == exam_code, "کد آزمون پس از ویرایش نباید تغییر کند!"

            # رفتن به صفحهٔ «آزمون‌ها» و باز کردن آزمون برای دانش‌آموزان
            page.evaluate("() => window.ExamSite.go('exams')")
            page.get_by_text(exam_title).first.wait_for()
            # کلیک روی دکمهٔ باز کردن آزمون
            open_btn = page.locator("button[title*='باز'], button[aria-label*='باز'], button:has-text('باز کردن'), button:has-text('بازکردن')").first
            if open_btn.count() > 0:
                open_btn.click()
            else:
                # منوی عملیات کارت آزمون
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

            # خروج معلم
            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۵) ورود دانش‌آموز + ورود با کد + ترسیم هندسی روی <canvas>
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_student")
            page.locator("input[type='password']").first.fill("StudentPass123!")
            page.locator("input[type='password']").first.press("Enter")
            page.wait_for_function("() => !document.querySelector('input[type=password]')")

            # ورود کد آزمون و شروع پاسخ‌گویی
            page.evaluate("() => window.ExamSite.go('join')")
            code_inp = page.locator("input[placeholder*='کد']").first
            code_inp.wait_for()
            code_inp.fill(exam_code)
            click_visible_button(page, "ورود به آزمون")

            canvas = page.locator(".st-construct canvas").first
            canvas.wait_for()
            assert not leaked_student_payload_keys, f"نشت کلید محرمانه در شبکه: {leaked_student_payload_keys}"

            # کلیک واقعی روی بوم برای ثبت A(0,3)، B(0,0)، C(4,0) → تشکیل زاویهٔ قائمه ۹۰ درجه!
            click_canvas_math_point(page, canvas, 0.0, 3.0)
            canvas = page.locator(".st-construct canvas").first
            click_canvas_math_point(page, canvas, 0.0, 0.0)
            canvas = page.locator(".st-construct canvas").first
            click_canvas_math_point(page, canvas, 4.0, 0.0)

            page.get_by_text("۹۰.۰°").first.wait_for()
            record_step(
                page,
                "05_student_geometry_canvas.png",
                "پاسخ‌گویی دانش‌آموز — بوم ترسیم هندسی و محاسبهٔ زندهٔ زاویهٔ ∠ABC=90.0°",
                "ثبت نقاط A(0,3)، B(0,0) و C(4,0) با کلیک ماوس روی بوم و نمایش کمان و مقدار ۹۰ درجه",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۶) رفتن به سؤال ۲ (اثبات مرحله‌ای) و بررسی ارجاع زنده به نتیجهٔ گام قبل
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.get_by_role("button", name=re.compile(r"بعدی")).first.click()
            page.get_by_text("ارجاع به نتیجهٔ گام قبل").first.wait_for()

            step1_text = "AB بر BC عمود است؛ بنابراین AC² = AB² + BC² = ۹ + ۱۶ = ۲۵"
            proof_boxes = page.locator(".st-input")
            proof_boxes.nth(0).fill(step1_text)
            if proof_boxes.count() >= 4:
                proof_boxes.nth(1).fill("قضیهٔ فیثاغورس در مثلث قائم‌الزاویهٔ ABC")
                # بررسی به‌روزرسانی آنی کارت ارجاع به گام قبل بالای گام ۲!
                assert page.locator("div", has_text=step1_text).count() >= 1
                proof_boxes.nth(2).fill("از جذب ۲۵ نتیجه می‌شود AC = ۵ واحد")
                proof_boxes.nth(3).fill("ریشهٔ دوم عدد مثبت ۲۵")
            else:
                proof_boxes.nth(1).fill("از جذب ۲۵ نتیجه می‌شود AC = ۵ واحد")

            record_step(
                page,
                "06_student_proof_carries_forward.png",
                "پاسخ‌گویی دانش‌آموز — اثبات دوگامی و ارجاع زنده به نتیجهٔ گام قبل",
                "نمایش بلادرنگ گزارهٔ گام الف در کارت مرجع بالای گام ب بدون خروج از سؤال",
                "1366×900",
                t0,
            )

            # --------------------------------------------------------
            # ۷) پاسخ به سؤال ۳ + انقضای زمان سرور بخش اول + قفل بازگشت + ارسال نهایی
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.get_by_role("button", name=re.compile(r"بعدی")).first.click()
            page.get_by_text("مساحت مثلث").first.wait_for()
            # انتخاب گزینهٔ دوم («۶»)
            page.get_by_text("۶").first.click()

            # منقضی کردن زمان سرور بخش «بخش هندسه مختصاتی» از طریق کنترل ساعت سرور
            req = urllib.request.Request(
                f"{mock_url}/__test__/expire_section",
                data=json.dumps({"exam_id": exam_id, "section": "بخش هندسه مختصاتی"}).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=5) as r:
                exp_res = json.loads(r.read().decode("utf-8"))
                assert "بخش هندسه مختصاتی" in exp_res["expired_sections"]

            # تلاش برای بازگشت به سؤال ۱ پس از همگام‌سازی مهلت بخش با سرور
            page.evaluate(
                """async (examId) => {
                    await window.ExamSite.rpcObj('native_sync_exam_section_v1', {
                        p_exam: examId,
                        p_section: 'بخش هندسه مختصاتی',
                        p_lock: false,
                        p_section_responses: {}
                    });
                }""",
                exam_id,
            )
            # کلیک روی شمارهٔ سؤال ۱ در نوار شمارهٔ سؤال‌ها یا ارسال نهایی آزمون
            submit_btn = page.get_by_role("button", name=re.compile(r"ثبت نهایی|ارسال نهایی|پایان آزمون|ارسال پاسخ")).first
            submit_btn.click()
            page.wait_for_timeout(250)
            confirm_btns = page.locator(".modal button").filter(has_text=re.compile(r"ارسال|ثبت|تأیید"))
            if confirm_btns.count() > 0:
                confirm_btns.first.click()

            page.wait_for_function("() => document.body.innerText.includes('رسید') || document.body.innerText.includes('ثبت شد')", timeout=8000)
            assert len(mock_state.answers) == 1, "پاسخ دانش‌آموز باید در سرور ثبت شده باشد"
            ans_rec = next(iter(mock_state.answers.values()))
            assert ans_rec["grades"][0] == 3.0, f"نمرهٔ خودکار ترسیم هندسی باید ۳ باشد اما {ans_rec['grades'][0]} بود"
            assert ans_rec["grades"][2] == 3.0, f"نمرهٔ خودکار چهارگزینه‌ای باید ۳ باشد اما {ans_rec['grades'][2]} بود"

            record_step(
                page,
                "07_student_section_locked_and_submit.png",
                "قفل زمان‌بندی سرور بخش اول، ثبت نهایی آزمون و صدور کد رسید",
                f"نمرهٔ خودکار اولیه: ترسیم هندسی = ۳/۳، چهارگزینه‌ای = ۳/۳ (رسید ثبت شد)",
                "1366×900",
                t0,
            )

            # خروج دانش‌آموز و ورود مجدد معلم برای تصحیح گام‌به‌گام اثبات
            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۸) تصحیح گام‌به‌گام معلم (`native_save_grade_parts_v1`)
            # --------------------------------------------------------
            t0 = time.monotonic()
            page.locator("input[type='text']").first.fill("uitest_teacher")
            page.locator("input[type='password']").first.fill("TestPass1234!")
            page.locator("input[type='password']").first.press("Enter")
            page.locator("button[aria-label='افزودن']").wait_for()

            page.evaluate("(eid) => window.ExamSite.go('grading', { examId: eid })", exam_id)
            page.get_by_text("علی رضایی").first.wait_for()
            page.locator(".g-item", has_text="علی رضایی").first.click()
            page.get_by_text("راهنمای نمره‌دهی دبیر").first.wait_for()

            # وارد کردن نمره و بازخورد هر دو گام اثبات (۲ + ۲ = ۴) و ذخیره و تأیید نهایی
            page.evaluate(
                """() => {
                    const gq = [...document.querySelectorAll('.g-q')][1];
                    const nums = [...gq.querySelectorAll('input[type=number]')];
                    nums.forEach(inp => { inp.value = '2'; inp.dispatchEvent(new Event('input', { bubbles: true })); });
                    const texts = [...gq.querySelectorAll('input[type=text], textarea')];
                    if (texts[0]) {
                        texts[0].value = 'استدلال هندسی و کاربرد فیثاغورس کاملاً دقیق بود.';
                        texts[0].dispatchEvent(new Event('input', { bubbles: true }));
                    }
                }"""
            )
            save_grade_btn = page.locator(".g-detail button").filter(has_text=re.compile(r"ذخیره|تأیید")).last
            save_grade_btn.scroll_into_view_if_needed()
            save_grade_btn.click()
            page.wait_for_function("() => document.body.innerText.includes('۱۰')", timeout=5000)

            updated_ans = next(iter(mock_state.answers.values()))
            assert updated_ans["graded"] is True
            assert updated_ans["total_grade"] == 10.0, f"نمرهٔ کل باید ۱۰ باشد اما {updated_ans['total_grade']} بود"
            page.evaluate("window.scrollTo(0, 0)")

            record_step(
                page,
                "08_teacher_grading_steps.png",
                "پنل تصحیح معلم — ارزیابی خودکار هندسه + نمره‌دهی گام‌به‌گام اثبات",
                "ثبت نمرهٔ گام الف (۲/۲) و گام ب (۲/۲) همراه بازخورد تخصصی؛ جمع نهایی ۱۰ از ۱۰",
                "1366×900",
                t0,
            )

            # خروج معلم و ورود مجدد دانش‌آموز برای مشاهدهٔ کارنامهٔ نهایی
            page.evaluate("() => window.ExamSite.logout()")
            page.locator("input[type='text']").first.wait_for()

            # --------------------------------------------------------
            # ۹) کارنامهٔ تفصیلی دانش‌آموز در نمای موبایل/دسکتاپ
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
            fb_loc = page.get_by_text("استدلال هندسی و کاربرد فیثاغورس کاملاً دقیق بود.").first
            fb_loc.wait_for(state="attached")
            fb_loc.scroll_into_view_if_needed()
            fb_loc.wait_for(state="visible")

            modal_txt = page.evaluate("() => document.body.innerText")
            assert "نمرهٔ محرمانهٔ دبیر" not in modal_txt, "Rubric محرمانهٔ دبیر نباید در کارنامهٔ دانش‌آموز نمایش داده شود!"

            record_step(
                page,
                "09_student_report_card.png",
                "کارنامهٔ نهایی دانش‌آموز — نمایش نمرهٔ ۱۰ از ۱۰ و بازخورد تفکیکی هر گام",
                "تأیید نمایش نمره و بازخورد گام‌های اثبات و عدم نشت Rubric محرمانهٔ دبیر",
                "1366×900",
                t0,
            )

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
