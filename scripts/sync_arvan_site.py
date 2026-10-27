#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.39 — ابزار هم‌گام‌سازی دامنهٔ onlineexam.ir با نسخهٔ منتشرشدهٔ سایت.

چرا این ابزار؟
  دامنهٔ `onlineexam.ir` روی ابرک آروان با Caddy یک «کپی ثابت» از خروجی بیلد
  سرو می‌کند، ولی `.github/workflows/site.yml` فقط روی Cloudflare Pages منتشر
  می‌کند؛ نتیجه این‌که دامنه هر بار چند نسخه عقب می‌ماند (V335.39: دامنه روی
  V335.32 بود و Pages روی V335.38). این ابزار همان شکاف را می‌بندد.

سه کارکرد (بدون هیچ سکرت و کلیدی):
  1) --check      مقایسهٔ نسخهٔ دامنه با نسخهٔ منتشرشده و گزارش عقب‌ماندگی
  2) --mirror     ساخت آینهٔ کامل از سایت منتشرشده در پوشهٔ محلی (site_mirror/)
  3) --apply      rsync آینه (یا پوشهٔ آمادهٔ site_out) روی سرور + بارگذاری مجدد Caddy

نمونه‌ها:
  python3 scripts/sync_arvan_site.py --check
  python3 scripts/sync_arvan_site.py --mirror
  python3 scripts/sync_arvan_site.py --apply --ssh-key ~/.ssh/arvan
  python3 scripts/sync_arvan_site.py --apply --from-dir site_out --yes      # همان کاری که CI می‌کند

کد بازگشت: 0 سالم · 2 عقب‌ماندگی/خطا در بررسی · 3 خطای شبکه · 4 خطای آرگومان
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request

DEFAULT_SOURCE = "auto"
# V335.41 — منابع نامزد به ترتیب اولویت. در ایران pages.dev گاهی فقط IPv6 می‌دهد و
# WSL مسیر IPv6 ندارد ⇒ «Network is unreachable». اول IPv4 تحمیل می‌شود و اگر باز
# هم نشد، شاخهٔ آزمایشی همان پروژه با هشدار صریح آزموده می‌شود.
SOURCE_CANDIDATES = [
    "https://onlineexam-1q1.pages.dev",
    "https://staging.onlineexam-1q1.pages.dev",
]
DEFAULT_HOST = "root@95.38.160.168"
LIVE_URL = "https://onlineexam.ir/"
UA = "Mozilla/5.0 (compatible; onlineexam-site-sync/1.0; +https://onlineexam.ir)"

# دانه‌های ثابت — مسیرهایی که در index.html به‌صورت لینک نمی‌آیند ولی باید منتقل شوند
SEEDS = [
    "/", "/index.html", "/favicon.ico", "/robots.txt", "/sitemap.xml",
    "/status.html", "/terms.html", "/privacy.html", "/pay-return.html",
    "/pwa/manifest.webmanifest", "/pwa/sw.js", "/pwa/pay-return.html",
]
EXTS = (".js", ".css", ".png", ".svg", ".ico", ".webmanifest", ".woff2", ".woff", ".ttf", ".json", ".html", ".txt", ".xml")
# مسیرهای ریشه‌ای که هرگز نباید در آینه باشند (پاسخ‌های API/فانکشن)
SKIP_PREFIX = ("/functions/", "/rest/", "/auth/", "/storage/", "/realtime/", "/_next/", "/api/")
# Cloudflare Pages هنگام سرو، یک تگ آنالیتیکس به HTMLها تزریق می‌کند؛ در آینه و در
# کپی دامنه نباید بماند (منبع حقیقت، site_out محلی است و تمیز).
CF_BEACON_RE = re.compile(rb"<!-- Cloudflare Pages Analytics -->.*?<!-- Cloudflare Pages Analytics -->", re.S)


def strip_cf_beacon(path, body):
    if not path.endswith(".html"):
        return body
    return CF_BEACON_RE.sub(b"", body)


CANDIDATE_WEBROOTS = ["/var/www/onlineexam", "/var/www/onlineexam.ir", "/var/www/html", "/srv/onlineexam", "/srv/www/onlineexam"]


# ---------- کمکی‌ها ----------

def prefer_ipv4():
    """V335.41 — resolve با IPv4 اول. علت خطای کاربر: Errno 101 روی AAAA."""
    import socket
    _orig = socket.getaddrinfo
    socket.getaddrinfo = lambda host, port, family=0, *a, **k: _orig(host, port, socket.AF_INET, *a, **k)


def log(msg=""):
    print(msg, flush=True)


def sha(b):
    return hashlib.sha256(b).hexdigest()


def fetch(url, timeout=45):
    """GET با UA؛ کد وضعیت و محتوا را برمی‌گرداند (بدون استثنا برای 4xx)."""
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read(), r.headers.get("content-type", "")
    except urllib.error.HTTPError as e:
        return e.code, e.read() or b"", e.headers.get("content-type", "") if e.headers else ""
    except Exception as e:  # noqa: BLE001 — قطعی شبکه/SSL/تایم‌اوت نباید اسکریپت را با traceback بخواباند
        log(f"⚠️  خطای شبکه در {url}: {type(e).__name__}: {str(e)[:120]}")
        return 0, b"", ""


def norm(path):
    if not path.startswith("/"):
        path = "/" + path
    return path.split("#")[0].split("?")[0]


def discover_refs(text):
    """مسیرهای هم‌دامنهٔ ارجاع‌شده در HTML/CSS/JS را بیرون می‌کشد."""
    out = set()
    if not text:
        return out
    for pat in (r'(?:src|href)="(/[^"#?]+)"', r"url\((?:\"|')?(/[^)\"'#?]+)", r'"(\/[A-Za-z0-9._/-]+\.(?:js|css|png|svg|ico|webmanifest|woff2))"'):
        for m in re.finditer(pat, text):
            out.add(norm(m.group(1)))
    for m in re.finditer(r"(?:engines|panels)\.[0-9a-f]{8,16}\.js", text):
        out.add(norm("/" + m.group(0)))
    return {p for p in out if p.endswith(EXTS) and not p.startswith(SKIP_PREFIX)}


def build_hashes(index_html):
    """نام چانک‌های hash‌دار را از index استخراج می‌کند (اثر انگشت نسخه)."""
    js = sorted(set(re.findall(r"/?c\d+\.[0-9a-f]{12}\.js", index_html)))
    css = sorted(set(re.findall(r"/?s\d+\.[0-9a-f]{12}\.css", index_html)))
    pan = sorted(set(re.findall(r"/?p\d+\.[0-9a-f]{12}\.js", index_html)))
    return {
        "js": [j.lstrip("/") for j in js],
        "css": [c.lstrip("/") for c in css],
        "panels": [p.lstrip("/") for p in pan],
        "fingerprint": (js[0].split(".")[1] if js else "?") + "/" + (css[0].split(".")[1] if css else "?"),
    }


# ---------- ۱) mirror ----------

def mirror(source, outdir, allow_missing=False):
    """آینهٔ کاملِ سایت منتشرشده را در outdir می‌سازد. تعداد فایل‌ها را برمی‌گرداند."""
    source = source.rstrip("/")
    log(f"📥 منبع: {source}")
    status, index, ctype = fetch(source + "/")
    if status != 200 or not index:
        log(f"❌ دریافت index ناموفق: HTTP {status}")
        return None
    index_hash = sha(index)
    files = {"/index.html": strip_cf_beacon("/index.html", index)}  # تگ آنالیتیکس Pages نباید به دامنه برود
    seeds = list(SEEDS)
    # موتورها/پنل‌های قدیمی هم روی سرور بمانند تا کش کاربران قدیمی نشکند
    import glob
    for pat in ("site/engines.*.js", "site/panels.*.js"):
        for f in glob.glob(pat):
            seeds.append("/" + os.path.basename(f))
    queue = seeds + sorted(discover_refs(index.decode("utf-8", "replace")))
    seen = {"/", "/index.html"}
    missing, fallbacks = [], []
    rounds = 0
    while queue and rounds < 3:
        rounds += 1
        nxt = []
        for path in queue:
            if path in seen or path == "/":
                continue
            seen.add(path)
            url = source + path
            st, body, ct = fetch(url)
            if st != 200 or not body:
                missing.append(path)
                continue
            if sha(body) == index_hash and path != "/index.html":
                # Pages برای مسیر ناموجود، index را با کد ۲۰۰ برمی‌گرداند
                fallbacks.append(path)
                continue
            files[path] = strip_cf_beacon(path, body)
            if path.endswith((".html", ".js", ".css", ".webmanifest", ".json")):
                nxt.extend(sorted(discover_refs(body.decode("utf-8", "replace"))))
        queue = [p for p in nxt if p not in seen]
    # pay-return در ریشه: اگر نبود از نسخهٔ pwa/ کپی می‌شود (سکرت موجود PAY_CALLBACK_BASE دست‌نخورده بماند)
    if "/pay-return.html" not in files and "/pwa/pay-return.html" in files:
        files["/pay-return.html"] = files["/pwa/pay-return.html"]
        log("ℹ️  /pay-return.html در ریشه نبود؛ از /pwa/pay-return.html کپی شد.")
    if fallbacks:
        log(f"⚠️  {len(fallbacks)} مسیر ناموجود (پاسخ fallback به‌جای فایل): " + ", ".join(fallbacks[:6]))
    if missing:
        log(f"⚠️  {len(missing)} مسیر خطا داد: " + ", ".join(missing[:6]))
    # اعتبارسنجی ساختاری
    ih = index.decode("utf-8", "replace")
    h = build_hashes(ih)
    problems = []
    if len(h["js"]) < 5:
        problems.append(f"چانک‌های JS کم‌اند ({len(h['js'])} < 5)")
    if len(h["css"]) < 2:
        problems.append(f"فایل‌های CSS کم‌اند ({len(h['css'])} < 2)")
    if "/index.html" not in files:
        problems.append("index.html گرفته نشد")
    if problems and not allow_missing:
        log("❌ آینه معتبر نیست: " + " · ".join(problems))
        return None
    # نوشتن روی دیسک
    if os.path.isdir(outdir):
        shutil.rmtree(outdir)
    total = 0
    for path, body in files.items():
        dest = os.path.join(outdir, path.lstrip("/"))
        os.makedirs(os.path.dirname(dest) or outdir, exist_ok=True)
        with open(dest, "wb") as f:
            f.write(body)
        total += len(body)
    manifest = {p: {"sha256": sha(b), "size": len(b)} for p, b in sorted(files.items())}
    info = {"source": source, "fingerprint": h["fingerprint"], "hashes": h,
            "files": len(files), "bytes": total, "built_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
    with open(os.path.join(outdir, ".sync-info.json"), "w", encoding="utf-8") as f:
        json.dump(info, f, ensure_ascii=False, indent=2)
    with open(os.path.join(outdir, "MANIFEST.txt"), "w", encoding="utf-8") as f:
        f.write(f"# onlineexam site mirror — {info['built_at']}\n# source: {source}\n# fingerprint: {h['fingerprint']}\n")
        for p, m in manifest.items():
            f.write(f"{m['sha256']}  {m['size']:>9}  {p}\n")
    log(f"✅ آینه ساخته شد: {len(files)} فایل، {total/1024:.1f}KB → {outdir}/")
    log(f"   اثر انگشت نسخه: {h['fingerprint']}  (js={len(h['js'])} css={len(h['css'])} panels={len(h['panels'])})")
    return info


# ---------- ۲) check ----------

def resolve_source(source):
    """V335.41 — اگر منبع اصلی نرسید، نامزدهای بعدی با هشدار صریح امتحان می‌شوند."""
    if source not in (DEFAULT_SOURCE, SOURCE_CANDIDATES[0]):
        st, body, _ = fetch(source.rstrip("/") + "/")
        return source, (body if st == 200 and body else None)
    for cand in SOURCE_CANDIDATES:
        st, body, _ = fetch(cand.rstrip("/") + "/")
        if st == 200 and body:
            if cand != SOURCE_CANDIDATES[0]:
                log(f"⚠️  {SOURCE_CANDIDATES[0]} در دسترس نبود (احتمالاً نبود مسیر IPv6).")
                log(f"   مقایسه با {cand} انجام می‌شود؛ اگر شاخهٔ آزمایشی عقب‌تر باشد محافظه‌کارانه است.")
            return cand, body
    return source, None


def local_fingerprint():
    """V335.41 — اثر انگشت نسخهٔ همین مخزن، بدون شبکه.

    چانک‌های `c*.js` و `s*.css` تنها از محتوای `site/index.html` ساخته می‌شوند و به
    کلید anon وابسته نیستند (کلید در اسکریپت جداگانه تزریق می‌شود)؛ پس می‌توان
    اثر انگشت نسخهٔ منتشرشده را بدون دسترسی به pages.dev از خود مخزن به‌دست آورد.
    """
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    idx = os.path.join(root, "site", "index.html")
    if not os.path.isfile(idx):
        return None
    html = open(idx, encoding="utf-8", errors="ignore").read()
    styles = list(re.finditer(r"<style>([\s\S]*?)</style>", html))
    scripts = list(re.finditer(r"<script>([\s\S]*?)</script>", html))
    if not styles or not scripts:
        return None
    css = max(styles, key=lambda m: len(m.group(1))).group(1)
    js = max(scripts, key=lambda m: len(m.group(1))).group(1)
    ch, jh = sha(css.encode("utf-8"))[:12], sha(js.encode("utf-8"))[:12]
    # همان شکل خروجی build_hashes تا مقایسه معنا داشته باشد (js/css)
    return {"js": [f"c0.{jh}.js"], "css": [f"s0.{ch}.css"], "panels": [], "fingerprint": f"{jh}/{ch}"}


def check(source=DEFAULT_SOURCE, live=LIVE_URL, quiet=False):
    """دامنهٔ زنده را با نسخهٔ منتشرشده مقایسه می‌کند."""
    source, idx_pub = resolve_source(source)
    st1 = 200 if idx_pub else 0
    st2, idx_live, _ = fetch(live)
    if st2 != 200:
        log(f"❌ دسترسی به دامنهٔ زنده ناموفق — live HTTP {st2} · published HTTP {st1}")
        return 3
    if not idx_pub:
        liv = build_hashes(idx_live.decode("utf-8", "replace"))
        loc = local_fingerprint()
        st3, body3, _ = fetch(live.rstrip("/") + "/pay-return.html")
        pay_ok = st3 == 200 and sha(body3) != sha(idx_live)
        if not quiet:
            log(f"   دامنهٔ زنده      : {liv['fingerprint']}  js={len(liv['js'])} css={len(liv['css'])}")
            log(f"   /pay-return.html : {'موجود ✅' if pay_ok else 'نیست ❌ (پاسخ صفحهٔ فرود)'}")
            log("⚠️  نسخهٔ منتشرشده (pages.dev) از این شبکه در دسترس نیست — احتمالاً مسیر IPv6.")
        if not loc:
            log("❌ نه صفحات در دسترس است و نه site/index.html محلی؛ مقایسه ممکن نیست.")
            return 3
        if not quiet:
            log(f"   مقایسه با مخزن  : {loc['fingerprint']}  js={len(loc['js'])} css={len(loc['css'])}")
        same = liv["fingerprint"] == loc["fingerprint"]
        if not quiet:
            if same:
                log("✅ دامنه با نسخهٔ همین مخزن هم‌تراز است (بررسی بدون شبکه).")
            else:
                log("❌ دامنه با نسخهٔ مخزن فرق دارد — هم‌گام‌سازی لازم است:")
                log("   python3 scripts/build_site_out.py && python3 scripts/sync_arvan_site.py --apply --from-dir site_out --ssh-key ~/.ssh/arvan")
        return 0 if (same and pay_ok) else 2
    pub = build_hashes(idx_pub.decode("utf-8", "replace"))
    liv = build_hashes(idx_live.decode("utf-8", "replace"))
    same = pub["fingerprint"] == liv["fingerprint"]
    # وجود صفحهٔ بازگشت در ریشهٔ دامنه (پاسخ SPA = نبود فایل)
    st3, body3, _ = fetch(live.rstrip("/") + "/pay-return.html")
    pay_root_ok = st3 == 200 and sha(body3) != sha(idx_live)
    if not quiet:
        log(f"   منتشرشده (Pages) : {pub['fingerprint']}  js={len(pub['js'])} css={len(pub['css'])}")
        log(f"   دامنهٔ زنده      : {liv['fingerprint']}  js={len(liv['js'])} css={len(liv['css'])}")
        log(f"   /pay-return.html : {'موجود ✅' if pay_root_ok else 'نیست ❌ (پاسخ صفحهٔ فرود)'}")
        if same:
            log("✅ دامنه با نسخهٔ منتشرشده هم‌تراز است.")
        else:
            log("❌ دامنه عقب است/فرق دارد — هم‌گام‌سازی لازم است (--apply).")
    return 0 if (same and pay_root_ok) else 2


# ---------- ۳) apply ----------

def ssh_base(host, key=None, port=22):
    cmd = ["ssh", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15"]
    if key:
        cmd += ["-i", os.path.expanduser(key)]
    if port and int(port) != 22:
        cmd += ["-p", str(port)]
    return cmd + [host]


def detect_webroot(host, key=None, port=22):
    """ریشهٔ وب را از Caddyfile یا مسیرهای رایج پیدا می‌کند."""
    probe = ("cat /etc/caddy/Caddyfile 2>/dev/null; echo '---'; "
             + "; ".join(f"echo W:{p}; test -f {p}/index.html && echo YES:{p}" for p in CANDIDATE_WEBROOTS))
    try:
        r = subprocess.run(ssh_base(host, key, port) + [probe], capture_output=True, text=True, timeout=60)
    except Exception as e:  # noqa: BLE001
        log(f"⚠️  SSH برای تشخیص مسیر ناموفق بود: {e}")
        return None
    out = r.stdout or ""
    caddy, _, probe_out = out.partition("---")
    roots = re.findall(r"^\s*root\s+\*?\s*(\S+)", caddy, re.M)
    for cand in CANDIDATE_WEBROOTS:
        if f"YES:{cand}" in probe_out:
            if roots and cand not in roots:
                log(f"ℹ️  Caddy می‌گوید root={roots[0]} ولی index در {cand} است؛ {cand} انتخاب شد.")
            return cand
    if roots:
        return roots[0].strip("\"'")
    return None


def apply(src_dir, host, webroot=None, key=None, port=22, dry_run=False, yes=False, reload=True):
    if not os.path.isfile(os.path.join(src_dir, "index.html")):
        log(f"❌ {src_dir}/index.html نیست — چیزی برای ارسال وجود ندارد.")
        return 4
    if webroot in (None, "", "auto"):
        webroot = detect_webroot(host, key, port)
        if not webroot:
            log("❌ ریشهٔ وب پیدا نشد. با --webroot /مسیر/دقیق دوباره اجرا کنید.")
            return 4
        log(f"🔎 ریشهٔ وب تشخیص داده شد: {webroot}")
    src = os.path.abspath(src_dir).rstrip("/") + "/"
    files = sum(len(f) for _, _, f in os.walk(src))
    log(f"📤 {files} فایل از {src} → {host}:{webroot}/ {'(dry-run)' if dry_run else ''}")
    if not yes and not dry_run:
        try:
            ans = input("ادامه می‌دهید؟ فایل‌های اضافی روی سرور حذف می‌شوند [y/N] ").strip().lower()
        except EOFError:
            ans = ""
        if ans not in ("y", "yes", "بله", "ب"):
            log("لغو شد.")
            return 4
    cmd = ["rsync", "-az", "--delete", "--delay-updates", "--itemize-changes",
           "-e", " ".join(ssh_base(host, key, port)[:-1])]
    if dry_run:
        cmd.append("--dry-run")
    cmd += [src, f"{host}:{webroot}/"]
    log("$ rsync … (خروجی فقط تغییرات)")
    r = subprocess.run(cmd, capture_output=True, text=True)
    lines = [l for l in (r.stdout or "").splitlines() if l.strip()]
    for line in lines[:40]:
        log("   " + line)
    if len(lines) > 40:
        log(f"   … و {len(lines)-40} خط دیگر")
    if r.returncode != 0:
        log(f"❌ rsync ناموفق (کد {r.returncode}): {(r.stderr or '').strip()[:400]}")
        return 2
    if dry_run:
        return 0
    if reload:
        reload_cmd = "systemctl reload caddy 2>/dev/null || caddy reload --config /etc/caddy/Caddyfile 2>/dev/null || true"
        rr = subprocess.run(ssh_base(host, key, port) + [reload_cmd], capture_output=True, text=True)
        log("🔁 Caddy reload: " + ("انجام شد" if rr.returncode == 0 else "رد شد (نیازی به reload فایل ثابت نیست)"))
    log("✅ ارسال شد. بررسی هم‌ترازی:")
    for _ in range(3):
        time.sleep(3)
        rc = check(quiet=True)
        log("   وضعیت: " + ("هم‌تراز ✅" if rc == 0 else "هنوز فرق دارد، تلاش بعدی…"))
        if rc == 0:
            break
    return 0


# ---------- main ----------

def main():
    p = argparse.ArgumentParser(description="هم‌گام‌سازی دامنهٔ onlineexam.ir با نسخهٔ منتشرشدهٔ سایت (V335.39)")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--check", action="store_true", help="مقایسهٔ دامنه با نسخهٔ منتشرشده (پیش‌فرض)")
    g.add_argument("--mirror", action="store_true", help="ساخت آینهٔ محلی از سایت منتشرشده")
    g.add_argument("--apply", action="store_true", help="ارسال آینه/پوشهٔ آماده روی سرور")
    p.add_argument("--source", default=DEFAULT_SOURCE, help=f"نشانی سایت منتشرشده (پیش‌فرض {DEFAULT_SOURCE})")
    p.add_argument("--out", default="site_mirror", help="پوشهٔ آینه (پیش‌فرض site_mirror)")
    p.add_argument("--from-dir", default=None, help="به‌جای آینه، این پوشهٔ آماده (مثل site_out) را بفرست")
    p.add_argument("--build-local", action="store_true", help="site_out را از همین مخزن بساز و بفرست (بدون pages.dev)")
    p.add_argument("--target", default=DEFAULT_HOST, help=f"مقصد SSH (پیش‌فرض {DEFAULT_HOST})")
    p.add_argument("--webroot", default="auto", help="ریشهٔ وب روی سرور یا auto")
    p.add_argument("--ssh-key", default=os.environ.get("ARVAN_SSH_KEY_FILE"), help="مسیر کلید SSH")
    p.add_argument("--port", type=int, default=22)
    p.add_argument("--yes", "-y", action="store_true", help="بدون پرسش تأیید")
    p.add_argument("--dry-run", action="store_true", help="rsync آزمایشی (بدون تغییر)")
    p.add_argument("--no-reload", action="store_true")
    p.add_argument("--allow-missing", action="store_true", help="آینه را حتی با فایل‌های ناقص بساز")
    p.add_argument("--quiet", "-q", action="store_true")
    a = p.parse_args()

    prefer_ipv4()  # V335.41 — پیش از هر درخواست شبکه
    if a.source == "auto":
        a.source = SOURCE_CANDIDATES[0]
    if a.apply:
        src_dir = a.from_dir
        if not src_dir:
            if a.build_local:
                log("🏗  ساخت site_out محلی (بدون نیاز به pages.dev)…")
                r = subprocess.run([sys.executable, os.path.join("scripts", "build_site_out.py")])
                if r.returncode != 0:
                    log("❌ ساخت محلی ناموفق بود.")
                    return r.returncode
                src_dir = "site_out"
            elif not os.path.isfile(os.path.join(a.out, "index.html")):
                log("ℹ️  آینه موجود نیست؛ ابتدا ساخته می‌شود…")
                if not mirror(a.source, a.out, a.allow_missing):
                    log("ℹ️  منبع منتشرشده در دسترس نبود؛ ساخت محلی site_out امتحان می‌شود…")
                    r = subprocess.run([sys.executable, os.path.join("scripts", "build_site_out.py")])
                    if r.returncode != 0:
                        return 2
                    src_dir = "site_out"
            if not src_dir:
                src_dir = a.out
        return apply(src_dir, a.target, a.webroot, a.ssh_key, a.port, a.dry_run, a.yes, not a.no_reload)

    if a.mirror:
        info = mirror(a.source, a.out, a.allow_missing)
        return 0 if info else 2

    return check(a.source, quiet=a.quiet)


if __name__ == "__main__":
    sys.exit(main())
