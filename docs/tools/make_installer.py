#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""ساخت بستهٔ نصب apply_vXX.py از آخرین commit(ها).
استفاده (در ریشهٔ مخزن):  python3 docs/tools/make_installer.py V291 <commit_پایه> /مسیر/خروجی/apply_v291.py
FILES = فایل‌های افزوده/تغییریافته بین پایه و HEAD؛ REMOVE = فایل‌های حذف‌شده.
"""
import base64, gzip, io, os, re, subprocess, sys, tarfile

def main():
    ver, base, out = sys.argv[1], sys.argv[2], sys.argv[3]
    files = subprocess.check_output(["git", "diff", "--no-renames", "--name-only", "--diff-filter=AM", base, "HEAD"], text=True).split()
    rem = subprocess.check_output(["git", "diff", "--no-renames", "--name-only", "--diff-filter=D", base, "HEAD"], text=True).split()
    msg = subprocess.check_output(["git", "log", "-1", "--format=%s"], text=True).strip()
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w") as t:
        for f in files:
            t.add(f, arcname=f)
    payload = base64.b64encode(gzip.compress(buf.getvalue())).decode()
    here = os.path.dirname(os.path.abspath(__file__))
    s = open(os.path.join(here, "installer_template.py"), encoding="utf-8").read()
    def sub(pat, rep, flags=re.M):
        nonlocal s
        s, n = re.subn(pat, lambda m: rep, s, count=1, flags=flags)
        assert n == 1, pat
    sub(r"^VERSION.*$", "VERSION = " + repr(ver))
    sub(r"^COMMIT_MSG.*$", "COMMIT_MSG = " + repr(msg))
    sub(r"^FILES = .*?\]", "FILES = " + repr(files), re.M | re.S)
    sub(r"^REMOVE = .*$", "REMOVE = " + repr(rem))
    sub(r'^PAYLOAD = """.*?"""', 'PAYLOAD = """' + payload + '"""', re.M | re.S)
    s = s.replace("apply_vXX.py", os.path.basename(out))
    open(out, "w", encoding="utf-8").write(s)
    print(len(files), "file(s),", len(rem), "removed →", out)

if __name__ == "__main__":
    main()
