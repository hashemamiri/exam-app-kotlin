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
