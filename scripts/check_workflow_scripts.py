#!/usr/bin/env python3
"""V359.2 — هر اسکریپتی که گردش‌کار صدا می‌زند باید واقعاً موجود باشد.

چرا این نگهبان ساخته شد؟ در CI این خطا رخ داد:

    python3: can't open file '.../scripts/check_v357_sql_sync.py':
    [Errno 2] No such file or directory
    Error: Process completed with exit code 2.

یعنی گردش‌کار نگهبانی را صدا می‌زد که فایلش در مخزن نبود. پیام خام
پایتون هیچ راهنمایی نمی‌داد و ۱۶ نگهبانِ سالم پیش از آن اجرا شده
بودند تا به این یکی برسد.

ریشه: نصاب روی ریموت `reset --hard` می‌کند و فقط فایل‌های بستهٔ خودش
را می‌نویسد. اگر فایلی در شاخهٔ ریموت گم شده باشد و در بستهٔ جاری
نباشد، گم می‌ماند — ولی ارجاعش در گردش‌کار باقی است.

این نگهبان همهٔ ارجاع‌ها را یک‌جا و با پیام فارسی می‌سنجد و باید
**نخستین** گام حلقهٔ نگهبان‌ها باشد تا خطا زود و روشن دیده شود.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent


def main() -> int:
    wf_dir = ROOT / ".github" / "workflows"
    if not wf_dir.is_dir():
        print("WORKFLOW_SCRIPTS: SKIP — پوشهٔ گردش‌کار نیست")
        return 0

    # دو گونه ارجاع وجود دارد و هر دو باید سنجیده شوند:
    #   ۱) مستقیم:  python3 scripts/check_x.py
    #   ۲) حلقه‌ای: for g in check_a check_b; do python3 "scripts/$g.py"
    # گونهٔ دوم دقیقاً همان جایی بود که خطای واقعی رخ داد؛ اگر فقط
    # مسیرهای مستقیم سنجیده شوند، نگهبان بی‌فایده است (خودآزمون نشان داد).
    direct = re.compile(r"(?:scripts|docs/tools)/[A-Za-z0-9_]+\.py")
    # فقط سرِ حلقه را می‌گیریم و بدنه را دستی تا «; do» می‌خوانیم.
    # دلیل: الگوی یک‌تکه با finditer همپوشانی ندارد و یک «for» پایتونی
    # می‌توانست تمام متن تا حلقهٔ شل را ببلعد و آن را از دید پنهان کند.
    loop_start = re.compile(r"for\s+(\w+)\s+in\s")
    refs: dict[str, list[str]] = {}
    for wf in sorted(wf_dir.glob("*.yml")):
        text = wf.read_text(encoding="utf-8")
        for m in direct.finditer(text):
            refs.setdefault(m.group(0), []).append(wf.name)
        for m in loop_start.finditer(text):
            var = m.group(1)
            end = text.find("; do", m.end())
            if end == -1 or end - m.end() > 1200:
                continue
            body = text[m.end():end]
            # نکته: در همین فایل‌ها حلقهٔ «for p in [...]» پایتونی هم هست.
            # اگر نشانه‌های پایتون دیده شود یا فاصله تا «; do» زیاد باشد،
            # این یک حلقهٔ شل نیست و باید رد شود.
            if len(body) > 1200 or any(c in body for c in "[](){}"):
                continue
            names = body.replace("\\\n", " ").replace("\\", " ").split()
            if not names or not all(re.fullmatch(r"[A-Za-z0-9_]+", n) for n in names):
                continue
            tail = text[end:end + 600]
            used = re.findall(r"(scripts|docs/tools)/\$\{?" + var + r"\}?\.py", tail)
            if not used:
                continue
            folder = used[0]
            for n in names:
                refs.setdefault(f"{folder}/{n}.py", []).append(wf.name)

    missing = []
    print(f"WORKFLOW_SCRIPTS: {len(refs)} اسکریپت ارجاع‌شده بررسی می‌شود")
    for path in sorted(refs):
        if (ROOT / path).is_file():
            continue
        missing.append(path)
        where = ", ".join(sorted(set(refs[path])))
        print(f"  ✗ {path}  ← {where}")

    if missing:
        print()
        print(f"❌ {len(missing)} اسکریپت در مخزن نیست ولی گردش‌کار صدایش می‌زند.")
        print("   علت معمول: فایل در شاخهٔ ریموت گم شده و بستهٔ نصب آن را")
        print("   بازنگردانده است. راه حل: بستهٔ ترمیم را اجرا کنید که همهٔ")
        print("   فایل‌های افزوده‌شده از نسخهٔ پایه را دوباره می‌نویسد.")
        print("   بررسی در WSL:")
        for path in missing:
            print(f"     git ls-files {path}")
        return 1

    print("WORKFLOW_SCRIPTS: PASS — همهٔ اسکریپت‌های ارجاع‌شده موجودند")
    return 0


if __name__ == "__main__":
    sys.exit(main())
