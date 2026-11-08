#!/usr/bin/env python3
"""V356.1 — نگهبان انتخاب چانک پنل‌ها.

چرا لازم شد: فهرست نگه‌داشت (`site/panels_keep.txt`) چند چانک را در
`site/` نگه می‌دارد. اسکریپت انتشار و هرم آزمون، چانک را با «تازه‌ترین
زمان تغییر فایل» برمی‌داشتند. زمان تغییر فایل در checkout و کپی
جابه‌جا می‌شود، پس انتخاب تصادفی بود و می‌شد کد کهنهٔ پنل منتشر یا
آزموده شود — بی‌آنکه هیچ آزمونی بشکند.

قاعده: مرجع انتخاب باید `window.__PANELS_URL` در `site/index.html`
باشد، نه زمان تغییر فایل. و آن فایل باید واقعاً وجود داشته باشد.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
INDEX = ROOT / "site" / "index.html"

# مسیرهایی که چانک را انتخاب می‌کنند و باید از __PANELS_URL پیروی کنند
SELECTORS = [
    "scripts/build_site_out.py",
    "scripts/web_ui_e2e.py",
    ".github/workflows/site.yml",
]


def main() -> int:
    problems: list[str] = []

    if not INDEX.exists():
        print("check_site_chunk_ref: FAIL — site/index.html پیدا نشد")
        return 1
    html = INDEX.read_text(encoding="utf-8")
    m = re.search(r'window\.__PANELS_URL\s*=\s*"([^"]+)"', html)
    if not m:
        problems.append("site/index.html باید window.__PANELS_URL داشته باشد")
    else:
        name = pathlib.PurePosixPath(m.group(1)).name
        if not (ROOT / "site" / name).exists():
            problems.append(f"چانک مرجع وجود ندارد: site/{name}")
        else:
            print(f"  چانک مرجع: {name}")

    for rel in SELECTORS:
        path = ROOT / rel
        if not path.exists():
            problems.append(f"فایل انتخاب‌گر پیدا نشد: {rel}")
            continue
        text = path.read_text(encoding="utf-8")
        if "getmtime" not in text:
            continue  # این فایل اصلاً با زمان فایل کاری ندارد
        if "__PANELS_URL" not in text:
            problems.append(f"{rel} چانک را با زمان تغییر فایل برمی‌دارد و __PANELS_URL را نمی‌خواند")
            continue
        # خواندن __PANELS_URL باید پیش از هر انتخاب بر پایهٔ زمان باشد
        if text.index("__PANELS_URL") > text.index("getmtime"):
            problems.append(f"{rel} اول با زمان فایل انتخاب می‌کند، بعد __PANELS_URL را می‌خواند")

    if problems:
        print("check_site_chunk_ref: FAIL")
        for p in problems:
            print("  ✗ " + p)
        return 1
    print(f"check_site_chunk_ref: سالم — {len(SELECTORS)} انتخاب‌گر بررسی شد")
    return 0


if __name__ == "__main__":
    sys.exit(main())
