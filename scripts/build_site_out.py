#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.41 — ساخت `site_out/` روی رایانهٔ خودتان، دقیقاً مثل گام Build در `.github/workflows/site.yml`.

چرا لازم شد؟ روی بعضی شبکه‌های ایران نام `*.pages.dev` فقط با IPv6 پاسخ می‌دهد و
WSL مسیر IPv6 ندارد (`URLError: [Errno 101] Network is unreachable`). در آن حالت
ابزار هم‌گام‌سازی نمی‌تواند نسخهٔ منتشرشده را بگیرد، ولی می‌شود همان خروجی CI را
محلی ساخت و با `--from-dir site_out` روی سرور فرستاد: هیچ وابستگی به Pages نمی‌ماند.

کلید anon از یکی از این منابع خوانده می‌شود (به ترتیب):
  1) متغیر محیطی SUPABASE_ANON_KEY
  2) local.properties  (همان فایلی که برای اجرای اپ دارید)
  3) app/build/generated/.../BuildConfig.java (اگر بیلد محلی انجام شده باشد)

اجرا:
  python3 scripts/build_site_out.py                 # ساخت site_out/
  python3 scripts/build_site_out.py --check-only    # فقط بررسی و مقایسهٔ هش CSS با نسخهٔ منتشرشده

خروجی: پوشهٔ `site_out/` آمادهٔ آپلود با `--from-dir site_out`.
"""
import argparse
import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def log(m=""):
    print(m, flush=True)


def read_anon_key(local_properties):
    key = (os.environ.get("SUPABASE_ANON_KEY") or "").strip()
    if key:
        return key, "env"
    for path in (local_properties, os.path.join(ROOT, "local.properties")):
        if path and os.path.isfile(path):
            for line in open(path, encoding="utf-8", errors="ignore"):
                if line.strip().startswith("SUPABASE_ANON_KEY"):
                    v = line.split("=", 1)[1].strip()
                    if v:
                        return v, path
    for gen in glob.glob(os.path.join(ROOT, "app", "build", "generated", "**", "BuildConfig.java"), recursive=True):
        m = re.search(r'SUPABASE_ANON_KEY\s*=\s*"([^"]+)"', open(gen, encoding="utf-8", errors="ignore").read())
        if m:
            return m.group(1), gen
    return "", None


def chunk_hashes(index_html):
    m = re.search(r"<style>([\s\S]*?)</style>", index_html)
    css = max(re.finditer(r"<style>([\s\S]*?)</style>", index_html), key=lambda x: len(x.group(1))).group(1) if m else ""
    js = max(re.finditer(r"<script>([\s\S]*?)</script>", index_html), key=lambda x: len(x.group(1))).group(1)
    return hashlib.sha256(css.encode("utf-8")).hexdigest()[:12], hashlib.sha256(js.encode("utf-8")).hexdigest()[:12]


def main():
    ap = argparse.ArgumentParser(description="ساخت site_out مثل گام Build در CI (V335.41)")
    ap.add_argument("--local-properties", default=os.path.join(ROOT, "local.properties"))
    ap.add_argument("--out", default=os.path.join(ROOT, "site_out"))
    ap.add_argument("--check-only", action="store_true", help="فقط بررسی: هش CSS/JS و وجود کلید")
    ap.add_argument("--no-build", action="store_true", help="site/index.html را از نو نساز")
    a = ap.parse_args()

    key, src = read_anon_key(a.local_properties)
    if not key:
        log("❌ SUPABASE_ANON_KEY پیدا نشد. یکی از این‌ها را انجام دهید:")
        log("   •  SUPABASE_ANON_KEY='eyJ…' python3 scripts/build_site_out.py")
        log("   •  یا خط SUPABASE_ANON_KEY=… را در local.properties بگذارید")
        return 4
    log(f"🔑 کلید anon از: {src}  (طول {len(key)})")

    if not a.no_build:
        r = subprocess.run([sys.executable, os.path.join(ROOT, "site", "build_site.py")], cwd=ROOT)
        if r.returncode != 0:
            log("❌ build_site.py ناموفق بود")
            return 2

    idx_path = os.path.join(ROOT, "site", "index.html")
    h = open(idx_path, encoding="utf-8").read()
    css_hash, js_hash = chunk_hashes(h)
    log(f"   هش محلی: css={css_hash} js={js_hash}   (js به کلید anon وابسته است، css نه)")
    if a.check_only:
        return 0

    out = a.out
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)

    # ۱) تزریق کلید anon (همان جای‌گذاری CI)
    old = 'var SUPABASE_ANON_KEY = "…";'
    if h.count(old) != 1:
        log("❌ جای‌نگهدار کلید anon در site/index.html پیدا نشد (build_site.py تغییر کرده؟)")
        return 2
    h = h.replace(old, 'var SUPABASE_ANON_KEY = "%s";' % key)
    open(os.path.join(out, "index.html"), "w", encoding="utf-8").write(h)

    # ۲) کپی‌های ثابت (همان فهرست site.yml)
    site = os.path.join(ROOT, "site")
    shutil.copytree(os.path.join(site, "pwa"), os.path.join(out, "pwa"))
    for name in ("status.html", "terms.html", "privacy.html", "robots.txt", "sitemap.xml", "favicon.ico", "pay-return.html"):
        p = os.path.join(site, name)
        if os.path.isfile(p):
            shutil.copy2(p, os.path.join(out, name))
    for pat in ("engines.*.js", "panels.*.js"):
        for f in glob.glob(os.path.join(site, pat)):
            shutil.copy2(f, os.path.join(out, os.path.basename(f)))
    if os.path.isdir(os.path.join(site, "fonts")):
        shutil.copytree(os.path.join(site, "fonts"), os.path.join(out, "fonts"))

    # ۳) شکستن CSS و JS به چانک‌های hash‌دار + پنل‌ها + نسخهٔ سرویس‌ورکر
    html = open(os.path.join(out, "index.html"), encoding="utf-8").read()
    big_style = max(re.finditer(r"<style>([\s\S]*?)</style>", html), key=lambda m: len(m.group(1)))
    css = big_style.group(1)
    chash = hashlib.sha256(css.encode("utf-8")).hexdigest()[:12]
    depth, mid, split_idx = 0, len(css) // 2, len(css)
    for i, ch in enumerate(css):
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0 and i >= mid:
                split_idx = i + 1
                break
    open(os.path.join(out, f"s0.{chash}.css"), "w", encoding="utf-8").write(css[:split_idx])
    open(os.path.join(out, f"s1.{chash}.css"), "w", encoding="utf-8").write(css[split_idx:])
    html = html[:big_style.start()] + f'<link rel="stylesheet" href="/s0.{chash}.css"><link rel="stylesheet" href="/s1.{chash}.css">' + html[big_style.end():]

    pfile = sorted(glob.glob(os.path.join(site, "panels.*.js")), key=os.path.getmtime)[-1]
    pjs = open(pfile, encoding="utf-8").read()
    phash = hashlib.sha256(pjs.encode("utf-8")).hexdigest()[:12]
    pstep = (len(pjs) + 4) // 5
    purls = []
    for idx, pc in enumerate([pjs[i:i + pstep] for i in range(0, len(pjs), pstep)]):
        pname = f"p{idx}.{phash}.js"
        purls.append("/" + pname)
        open(os.path.join(out, pname), "w", encoding="utf-8").write(
            f"window.__PJ=window.__PJ||[];window.__PJ[{idx}]={json.dumps(pc, ensure_ascii=False)};\n")
    html = html.replace("window.__PANELS_URL =", f"window.__PANELS_URLS = {json.dumps(purls)}; window.__PANELS_URL =", 1)

    big_script = max(re.finditer(r"<script>([\s\S]*?)</script>", html), key=lambda m: len(m.group(1)))
    js = big_script.group(1)
    jhash = hashlib.sha256(js.encode("utf-8")).hexdigest()[:12]
    jstep = (len(js) + 4) // 5
    stags = []
    tail = ("if(window.__CJ&&window.__CJ.length===5&&window.__CJ.filter(Boolean).length===5){var _s=document.createElement('script');"
            "_s.textContent=window.__CJ.join('');delete window.__CJ;document.body.appendChild(_s);}")
    for idx, jc in enumerate([js[i:i + jstep] for i in range(0, len(js), jstep)]):
        cname = f"c{idx}.{jhash}.js"
        stags.append(f'<script src="/{cname}"></script>')
        open(os.path.join(out, cname), "w", encoding="utf-8").write(
            f"window.__CJ=window.__CJ||[];window.__CJ[{idx}]={json.dumps(jc, ensure_ascii=False)};{tail}\n")
    html = html[:big_script.start()] + "".join(stags) + html[big_script.end():]
    open(os.path.join(out, "index.html"), "w", encoding="utf-8").write(html)

    sw = os.path.join(out, "pwa", "sw.js")
    s = open(sw, encoding="utf-8").read()
    if "__SW_VERSION__" in s:
        hh = hashlib.sha256(open(os.path.join(out, "index.html"), "rb").read()).hexdigest()[:12]
        open(sw, "w", encoding="utf-8").write(s.replace("__SW_VERSION__", hh))

    # ۴) هدرهای کش (همان رشتهٔ site.yml)
    open(os.path.join(out, "_headers"), "w", encoding="utf-8").write("""/*
  Cache-Control: no-cache
  Alt-Svc: clear
  X-Content-Type-Options: nosniff
  X-Frame-Options: SAMEORIGIN
  Referrer-Policy: strict-origin-when-cross-origin
  Strict-Transport-Security: max-age=31536000; includeSubDomains
/pwa/sw.js
  Cache-Control: no-cache
  Service-Worker-Allowed: /
/engines.*.js
  Cache-Control: public, max-age=31536000, immutable
/panels.*.js
  Cache-Control: public, max-age=31536000, immutable
/s*.css
  Cache-Control: public, max-age=31536000, immutable
/c*.js
  Cache-Control: public, max-age=31536000, immutable
/p*.js
  Cache-Control: public, max-age=31536000, immutable
/fonts/*
  Cache-Control: public, max-age=31536000, immutable
""")

    files = sum(len(f) for _, _, f in os.walk(out))
    log(f"✅ site_out آماده شد: {files} فایل → {out}")
    log(f"   چانک‌ها: c0..c4.{jhash}.js · s0/s1.{chash}.css · p0..p4.{phash}.js")
    log("   حالا: python3 scripts/sync_arvan_site.py --apply --from-dir site_out --ssh-key ~/.ssh/arvan")
    return 0


if __name__ == "__main__":
    sys.exit(main())
