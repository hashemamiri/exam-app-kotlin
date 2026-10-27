#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V335.45 — افزودن مسیرهای حساس به فهرست no-cache در Caddyfile سرور، به‌صورت ایمن و تکرارپذیر.

چرا؟ صفحهٔ بازگشت درگاه (`/pay-return.html`) در Caddyfile فعلی داخل `@nocache` نبود؛
مرورگر می‌تواند نسخهٔ کش‌شدهٔ آن را پس از آپدیت‌ها نشان دهد. این ابزار همان یک خط را
به همهٔ بلوک‌های `@nocache path …` اضافه می‌کند.

ایمنی‌ها:
  • فایل اصلی با مهر زمانی پشتیبان می‌شود؛
  • نسخهٔ تازه **قبل از** جایگزینی با `caddy validate` سنجیده می‌شود؛
  • اگر اعتبارسنجی رد شد، هیچ تغییری روی فایل اصلی انجام نمی‌شود؛
  • با `--dry-run` فقط diff نمایش داده می‌شود؛
  • اجرای دوباره بی‌اثر است (idempotent).

نمونه:
  python3 scripts/caddy_ensure_nocache.py --ssh-key ~/.ssh/arvan --dry-run
  python3 scripts/caddy_ensure_nocache.py --ssh-key ~/.ssh/arvan
"""
import argparse
import difflib
import os
import re
import subprocess
import sys
import time

DEFAULT_HOST = "root@95.38.160.168"
CADDYFILE = "/etc/caddy/Caddyfile"
WANTED = ["/pay-return.html"]
RESET_HINT = "/reset.html"     # اگر بلوک @nocache اصلاً نبود، پیشنهاد می‌شود
NOCACHE_RE = re.compile(r"^(\s*)@nocache\s+path\s+(.*)$")


def log(m=""):
    print(m, flush=True)


def ssh_cmd(host, key, port=22):
    cmd = ["ssh", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15"]
    if key:
        cmd += ["-i", os.path.expanduser(key)]
    if port and int(port) != 22:
        cmd += ["-p", str(port)]
    return cmd + [host]


def run_remote(host, key, command, stdin=None, port=22):
    return subprocess.run(ssh_cmd(host, key, port) + [command], input=stdin,
                          capture_output=True, text=True, timeout=120)


def patch(text, wanted):
    """همهٔ خطوط @nocache را تکمیل می‌کند؛ خروجی (متن تازه، تعداد تغییر)."""
    lines = text.splitlines()
    changed = 0
    for i, line in enumerate(lines):
        m = NOCACHE_RE.match(line)
        if not m:
            continue
        tokens = m.group(2).split()
        missing = [w for w in wanted if w not in tokens]
        if missing:
            lines[i] = line.rstrip() + " " + " ".join(missing)
            changed += 1
    out = "\n".join(lines)
    if text.endswith("\n"):
        out += "\n"
    return out, changed


def main():
    ap = argparse.ArgumentParser(description="افزودن مسیرهای حساس به @nocache در Caddyfile (V335.45)")
    ap.add_argument("--target", default=DEFAULT_HOST)
    ap.add_argument("--ssh-key", default=os.environ.get("ARVAN_SSH_KEY_FILE"))
    ap.add_argument("--port", type=int, default=22)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--no-reload", action="store_true")
    a = ap.parse_args()

    if a.ssh_key and not os.path.isfile(os.path.expanduser(a.ssh_key)):
        log(f"❌ کلید SSH پیدا نشد: {a.ssh_key}")
        log("   ssh-keygen -t ed25519 -f ~/.ssh/arvan -N \"\"  و  ssh-copy-id -i ~/.ssh/arvan.pub root@…")
        return 4

    r = run_remote(a.target, a.ssh_key, f"cat {CADDYFILE} 2>/dev/null", port=a.port)
    if r.returncode != 0 or not r.stdout.strip():
        log(f"❌ خواندن {CADDYFILE} ناموفق: {(r.stderr or '').strip()[:200]}")
        return 3
    original = r.stdout

    new, changed = patch(original, WANTED)
    if not changed:
        log("✅ از قبل درست است؛ هیچ تغییری لازم نبود (همهٔ خطوط @nocache این مسیرها را دارند).")
        return 0

    diff = list(difflib.unified_diff(original.splitlines(), new.splitlines(),
                                     fromfile=CADDYFILE, tofile=CADDYFILE + " (پیشنهادی)", lineterm=""))
    if diff:
        log("📝 تغییر پیشنهادی:")
        for line in diff:
            log("   " + line)
    log(f"   ({changed} خط @nocache تکمیل می‌شود)")

    if a.dry_run:
        log("\n(--dry-run) چیزی ارسال نشد.")
        return 0

    stamp = time.strftime("%Y%m%d-%H%M%S")
    remote_new = f"/tmp/Caddyfile.v33545.{stamp}"
    log(f"\n📤 ارسال نسخهٔ پیشنهادی به {remote_new} …")
    w = run_remote(a.target, a.ssh_key, f"cat > {remote_new}", stdin=new, port=a.port)
    if w.returncode != 0:
        log(f"❌ نوشتن فایل موقت ناموفق: {(w.stderr or '').strip()[:200]}")
        return 2

    log("🧪 اعتبارسنجی با caddy validate (بدون تغییر فایل اصلی) …")
    v = run_remote(a.target, a.ssh_key, f"caddy validate --config {remote_new} --adapter caddyfile 2>&1 | tail -3", port=a.port)
    out = (v.stdout or "").strip()
    if v.returncode != 0 or "Valid configuration" not in out:
        log("❌ اعتبارسنجی رد شد — فایل اصلی دست‌نخورده ماند.")
        log("   " + out.replace("\n", "\n   ")[:600])
        return 2
    log("✅ Valid configuration")

    backup = f"{CADDYFILE}.bak-{stamp}"
    log(f"💾 پشتیبان: {backup}")
    steps = (f"cp {CADDYFILE} {backup} && "
             f"cp {remote_new} {CADDYFILE} && chmod 644 {CADDYFILE}")
    s = run_remote(a.target, a.ssh_key, steps, port=a.port)
    if s.returncode != 0:
        log(f"❌ جایگزینی ناموفق: {(s.stderr or '').strip()[:200]}")
        return 2

    if not a.no_reload:
        log("🔁 systemctl reload caddy …")
        rl = run_remote(a.target, a.ssh_key, "systemctl reload caddy && systemctl is-active caddy", port=a.port)
        log("   " + ((rl.stdout or "").strip() or (rl.stderr or "").strip())[:120])
        if rl.returncode != 0:
            log("⚠️ reload ناموفق؛ بازگردانی از پشتیبان…")
            run_remote(a.target, a.ssh_key, f"cp {backup} {CADDYFILE} && systemctl reload caddy", port=a.port)
            return 2

    log(f"✅ انجام شد. پشتیبان: {backup}")
    log("   تأیید: curl -sI https://onlineexam.ir/pay-return.html | grep -i cache-control")
    return 0


if __name__ == "__main__":
    sys.exit(main())
