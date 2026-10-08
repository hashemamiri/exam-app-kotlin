#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""نصاب خوداستخراج نسخهٔ VERSION (قاعدهٔ §11 سند handoff).
اجرا:  cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && python3 /mnt/c/Users/Hashem/Downloads/apply_vXX.py
"""
import base64, gzip, io, os, pathlib, subprocess, sys, tarfile

VERSION = 'VXX'
COMMIT_MSG = 'VXX — ...'
FILES = []
PAYLOAD = """"""


def run(cmd, check=True, **kw):
    print('$', ' '.join(cmd), flush=True)
    return subprocess.run(cmd, check=check, text=True, **kw)


def main():
    root = pathlib.Path.cwd()
    if not (root / 'app' / 'build.gradle.kts').is_file() or not (root / '.git').exists():
        print('خطا: این دستور باید داخل پوشهٔ پروژهٔ exam-app-kotlin اجرا شود.')
        print('cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin')
        sys.exit(1)
    data = base64.b64decode(PAYLOAD.encode('ascii'))
    with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as tar:
        names = [m.name for m in tar.getmembers() if m.isfile()]
        for n in names:
            if n.startswith('/') or '..' in n.split('/'):
                print('مسیر نامعتبر در بسته:', n); sys.exit(1)
        tar.extractall(root)
    missing = [f for f in FILES if not (root / f).is_file()]
    if missing:
        print('خطا: این فایل‌ها استخراج نشدند:', missing); sys.exit(1)
    print(f'{VERSION}: {len(names)} فایل استخراج شد.')
    r = run([sys.executable, 'scripts/verify_native_final.py'], check=False)
    if r.returncode != 0:
        print('خطا: verify_native_final.py ناموفق بود؛ کامیت انجام نشد.'); sys.exit(1)
    run(['git', 'add', '-A'])
    r = run(['git', 'diff', '--cached', '--check'], check=False)
    if r.returncode != 0:
        print('خطا: git diff --check مشکل فاصلهٔ اضافی گزارش داد؛ کامیت انجام نشد.'); sys.exit(1)
    # پاک کردن خود نصاب اگر داخل پوشهٔ پروژه کپی شده باشد
    me = pathlib.Path(__file__).resolve()
    try:
        if me.is_relative_to(root.resolve()):
            me.unlink(); run(['git', 'add', '-A'])
    except Exception:
        pass
    r = run(['git', 'commit', '-m', COMMIT_MSG], check=False)
    if r.returncode != 0:
        print('توجه: چیزی برای کامیت نبود (احتمالاً قبلاً اعمال شده)؛ ادامه با push.')
    run(['git', 'push', 'origin', 'HEAD'])
    try:
        if me.exists() and not me.is_relative_to(root.resolve()):
            me.unlink()
    except Exception:
        pass
    print(f'✅ {VERSION} اعمال، کامیت و push شد.')


if __name__ == '__main__':
    main()
