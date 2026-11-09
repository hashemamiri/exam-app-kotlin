#!/usr/bin/env python3
"""V357.1 — فایل یک‌جای نصب باید با دو فایل منبع هم‌خوان بماند.

سه فایل SQL که با دست نگه داشته شوند، دیر یا زود از هم جدا می‌افتند و
کاربر فایل کهنه را اجرا می‌کند. این نگهبان _ALL را بازتولید و مقایسه
می‌کند.

اجرا: python3 scripts/check_v357_sql_sync.py
"""
import subprocess
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent


def main() -> int:
    r = subprocess.run([sys.executable, str(ROOT / "scripts" / "build_v357_sql.py"), "--check"],
                       capture_output=True, text=True, cwd=str(ROOT))
    sys.stdout.write(r.stdout)
    if r.stderr.strip():
        sys.stderr.write(r.stderr)
    return r.returncode


if __name__ == "__main__":
    raise SystemExit(main())
