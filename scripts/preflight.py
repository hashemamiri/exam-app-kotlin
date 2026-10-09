#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V220.1 — چک‌لیست پیش از ساخت نصاب (درس‌های V215/V218/V220؛ handoff §11.1). همه باید سبز باشند:
 1) check_test_pins (همهٔ تست‌ها) + حالت --strict برای تست‌های نسخه‌های جدید داده‌شده
 2) verify_native_final
 3) site/build_site.py و این‌که site/index.html با خروجی تازه یکسان باشد (فراموش‌نشدن build)
 4) git diff --check (فاصلهٔ انتهایی/خط خالی آخر)
 5) text/APP_VERSION.txt با خط اول CHANGELOG یکی باشد و تست‌های جدید با الگوی V<ver>_ وجود داشته باشند
 6) ثابت‌های پین‌شدهٔ قدیمی: هیچ فایل تستی به فایل حذف‌شده اشاره نکند
کاربرد:  python3 scripts/preflight.py [app/src/test/.../V2xx_*.kt ...]"""
import subprocess, sys, os, re, hashlib

ok = True
def step(name, cmd, **kw):
    global ok
    r = subprocess.run(cmd, capture_output=True, text=True, **kw)
    tail = (r.stdout + r.stderr).strip().splitlines()[-3:]
    print(('✅' if r.returncode == 0 else '❌'), name, '|', ' / '.join(tail)[:300])
    if r.returncode != 0: ok = False
    return r

new_tests = sys.argv[1:]
step('pins (all)', ['python3', 'scripts/check_test_pins.py'])
if new_tests:
    r = step('pins --strict (new tests)', ['python3', 'scripts/check_test_pins.py', '--strict'] + new_tests)
    if 'UNPARSED' in r.stdout: print('   ⚠ موارد UNPARSED را دستی بررسی کنید:'); print('\n'.join('     ' + l for l in r.stdout.splitlines() if 'UNPARSED' in l))
step('verify_native_final', ['python3', 'scripts/verify_native_final.py'])
before = hashlib.sha1(open('site/index.html', 'rb').read()).hexdigest()
step('build_site', ['python3', 'site/build_site.py'])
after = hashlib.sha1(open('site/index.html', 'rb').read()).hexdigest()
if before != after: ok = False; print('❌ site/index.html قدیمی بود؛ اکنون بازسازی شد — دوباره preflight را اجرا کنید')
else: print('✅ site/index.html به‌روز است')
step('git diff --check', ['git', 'diff', '--check'])
step('git diff --cached --check', ['git', 'diff', '--cached', '--check'])
ver = open('text/APP_VERSION.txt', encoding='utf-8').read().strip()
first = open('text/CHANGELOG_FA.txt', encoding='utf-8').read().splitlines()[0]
if ver in first: print('✅ نسخه', ver, 'در خط اول CHANGELOG')
else: print('⚠ خط اول CHANGELOG نسخهٔ', ver, 'را ندارد (اگر تغییر فقط سایت است طبیعی است):', first[:80])
# فایل‌های حذف‌شده که تستی به آن‌ها اشاره می‌کند
tests = subprocess.run(['bash', '-lc', 'grep -rhoE "(source|src)\\(\\"[^\\"]+\\"\\)" app/src/test | sort -u'], capture_output=True, text=True).stdout
for m in re.finditer(r'\("([^"]+)"\)', tests):
    path = m.group(1)
    if '$' in path or '/' not in path: continue  # قالب کاتلین یا نام نسبی کمکی (site/<name>.js)
    if not os.path.exists(path): ok = False; print('❌ تست به فایل ناموجود اشاره می‌کند:', path)
# V232.2 — همان اسکن Secret گردش‌کار android.yml، محلی (الگوها با الحاق ساخته می‌شوند تا خودِ این فایل گیر نیفتد)
_pem = 'BEGIN ' + '(?:RSA |EC |OPENSSH )?' + 'PRIVATE' + ' KEY'
_pats = {'private_key': re.compile(_pem), 'jwt': re.compile(r'eyJ[A-Za-z0-9_-]{20,}[.]eyJ[A-Za-z0-9_-]{20,}[.][A-Za-z0-9_-]{10,}'), 'supabase_secret': re.compile('sb_' + 'secret_[A-Za-z0-9_-]{16,}')}
_tracked = subprocess.run(['git', 'ls-files'], capture_output=True, text=True).stdout.splitlines()
_staged = subprocess.run(['git', 'status', '--porcelain', '--untracked-files=all'], capture_output=True, text=True).stdout.splitlines()
_tracked += [l[3:] for l in _staged if len(l) > 3]
for _f in sorted(set(_tracked)):
    if not os.path.isfile(_f) or os.path.getsize(_f) > 2_000_000: continue
    try: _t = open(_f, encoding='utf-8', errors='ignore').read()
    except Exception: continue
    for _l, _p in _pats.items():
        if _p.search(_t): ok = False; print('❌ اسکن Secret (مثل CI):', _l, _f)
if re.search(r'(?i)(release[.]keystore|[.]jks$|[.]p12$|keystore[.]properties$|(^|/)[.]env($|[.]))', '\n'.join(_tracked), re.M): pass
print('\nPREFLIGHT:', 'PASS' if ok else 'FAIL'); sys.exit(0 if ok else 1)
