#!/usr/bin/env python3
"""V210 — شمارهٔ نسخهٔ برنامه (text/APP_VERSION.txt، قالب a.bb.cc) را یکی زیاد می‌کند: 1.01.01 → 1.01.02 … 1.01.99 → 1.02.00
هر نصاب (apply_vXX.py) که CI اپ را اجرا می‌کند باید نسخهٔ جدید داشته باشد (Supabase هر کد نسخه را فقط یک بار می‌پذیرد)."""
import re, sys, pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "text" / "APP_VERSION.txt"
cur = p.read_text(encoding="utf-8").strip()
m = re.fullmatch(r"(\d{1,2})\.(\d{2})\.(\d{2})", cur)
if not m:
    print("قالب نسخه نادرست است:", cur); sys.exit(1)
a, b, c = (int(x) for x in m.groups())
c += 1
if c > 99:
    c = 0; b += 1
if b > 99:
    b = 0; a += 1
new = f"{a}.{b:02d}.{c:02d}"
p.write_text(new + "\n", encoding="utf-8")
print(f"{cur} → {new}")
