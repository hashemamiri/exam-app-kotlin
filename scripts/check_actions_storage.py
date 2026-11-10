#!/usr/bin/env python3
"""V359.1 — نگهبان مصرف فضای GitHub Actions.

چرا؟ سهمیهٔ رایگان ۵۱۲ مگابایت است. گزارش بصری هر اجرا ~۹ مگابایت
جا می‌گرفت و ماندگاری ۱۴ روزه پس از حدود ۲۸ اجرا فضا را پر می‌کرد.
کاربر به ۹۰٪ مصرف رسید.

این نگهبان دو چیز را پین می‌کند:
  ۱) هیچ آرتیفکتی ماندگاری بلند ندارد (پیش‌فرض گیت‌هاب ۹۰ روز است).
  ۲) گالری HTML تصاویر را دوباره جاسازی نمی‌کند.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAX_RETENTION = 7
fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:200]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def main() -> int:
    wf = ROOT / ".github" / "workflows"
    print("=== ماندگاری آرتیفکت‌ها ===")
    found = 0
    for p in sorted(wf.glob("*.yml")):
        text = p.read_text(encoding="utf-8")
        for m in re.finditer(
            r"uses: actions/upload-artifact@v\d+(.*?)(?=\n      - |\n  [a-z]|\Z)", text, re.S
        ):
            block = m.group(1)
            found += 1
            name = re.search(r"name:\s*(.+)", block)
            label = name.group(1).strip() if name else "?"
            ret = re.search(r"retention-days:\s*(\d+)", block)
            if ret is None:
                ok(f"{p.name}: {label[:40]} ماندگاری دارد", False,
                   "بدون retention-days ⇒ پیش‌فرض ۹۰ روز")
            else:
                days = int(ret.group(1))
                ok(f"{p.name}: {label[:40]} = {days} روز", days <= MAX_RETENTION,
                   f"بیش از سقف {MAX_RETENTION} روز")
    ok("آرتیفکتی پیدا شد", found > 0, found)

    print("\n=== گالری HTML تصاویر را تکرار نکند ===")
    e2e = (ROOT / "scripts" / "web_ui_e2e.py").read_text(encoding="utf-8")
    ok("جاسازی base64 تصویر حذف شده", "base64.b64encode(shot_path" not in e2e)
    ok("ارجاع نسبی به PNG", 'src="{img_src}"' in e2e)
    ok("دلیل تغییر مستند شده", "سهمیهٔ Actions نصف" in e2e)

    print("\n=== فشرده‌سازی ===")
    site = (wf / "site.yml").read_text(encoding="utf-8")
    ok("گزارش سایت فشرده می‌شود", "compression-level:" in site)
    ok("پر شدن سهمیه جلوی انتشار را نمیگیرد", "continue-on-error: true" in site)
    ok("گزارش بیش از یک روز نگه داشته نمیشود", "retention-days: 1" in site)

    print()
    if fails:
        print(f"ACTIONS_STORAGE: FAIL ({len(fails)} مورد)")
        return 1
    print("ACTIONS_STORAGE: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
