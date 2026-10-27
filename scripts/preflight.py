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
step('data_api_grants', ['python3', 'scripts/check_supabase_data_api_grants.py', '--quiet'])  # V324 — تغییر ۳۰ اکتبر Supabase
step('kotlin_braces', ['python3', 'scripts/check_kotlin_braces.py'])  # V325 — آکولاد نامتوازن فقط در CI دیده می‌شد
step('kotlin_escapes', ['python3', 'scripts/check_kotlin_escapes.py', 'app/src'])  # V335.32 — دنبالهٔ گریز نامجاز مثل \\{ فقط در CI دیده می‌شد
step('site_sync', ['python3', 'scripts/check_site_sync.py'])  # V335.39 — نگهبان هم‌گام‌سازی دامنهٔ onlineexam.ir با نسخهٔ منتشرشده
step('version_bump', ['python3', 'scripts/check_version_bump.py'])  # V335.40 — بدون بالا رفتن نسخه، job اندروید «قبلاً منتشر شده» می‌دهد
before = hashlib.sha1(open('site/index.html', 'rb').read()).hexdigest()
step('build_site', ['python3', 'site/build_site.py'])
after = hashlib.sha1(open('site/index.html', 'rb').read()).hexdigest()
if before != after: ok = False; print('❌ site/index.html قدیمی بود؛ اکنون بازسازی شد — دوباره preflight را اجرا کنید')
else: print('✅ site/index.html به‌روز است')
step('git diff --check', ['git', 'diff', '--check'])
step('git diff --cached --check', ['git', 'diff', '--cached', '--check'])
ver = open('text/APP_VERSION.txt', encoding='utf-8').read().strip()
_cl_lines = open('text/CHANGELOG_FA.txt', encoding='utf-8').read().splitlines()
first = _cl_lines[0] if _cl_lines else ''
if ver in first: print('✅ نسخه', ver, 'در خط اول CHANGELOG')
else: print('⚠ خط اول CHANGELOG نسخهٔ', ver, 'را ندارد (اگر تغییر فقط سایت است طبیعی است):', first[:80])
# V334.2 — آینهٔ دقیق تست‌های V208_ReleaseNotesTest و V322_MathToolsRobustnessTest روی CHANGELOG_FA.txt
_hdr_re = re.compile(r'^(V\d+(?:\.\d+)*)\s*(?:\(([^)]*)\))?\s*(?:[—\-–:]\s*(.*))?$')
_blocks = []
for _raw in _cl_lines[:60]:
    _ln = _raw.strip().replace('`', '')
    if not _ln: continue
    _hm = _hdr_re.match(_ln)
    if _hm:
        _blocks.append({'ver': _hm.group(1), 'scope': (_hm.group(2) or '').strip(), 'summary': (_hm.group(3) or '').strip(), 'bullets': []})
    elif _ln.startswith('-') or _ln.startswith('•'):
        if _blocks: _blocks[-1]['bullets'].append(_ln.lstrip('-•').strip())
if not _cl_lines or not _cl_lines[0].strip().startswith('V') or not _blocks:
    ok = False; print('❌ V208/V322: خط اول CHANGELOG باید با سرآیند نسخه (Vxxx) شروع شود')
else:
    _top = _blocks[0]
    _app_aff = (not _top['scope']) or (not (_top['scope'].startswith('فقط') and 'اپ' not in _top['scope']) and 'اپ' in _top['scope'])
    if not _app_aff:
        ok = False; print('❌ V322: بالاترین بلوک CHANGELOG باید شامل اپ باشد (appAffecting=true):', _top['scope'])
    if not (1 <= len(_top['bullets']) <= 6):
        ok = False; print('❌ V208/V322: تعداد بولت‌های بالاترین بلوک CHANGELOG باید بین ۱ تا ۶ باشد:', len(_top['bullets']))
    for _blk in _blocks[:3]:
        for _b in _blk['bullets']:
            if len(_b) > 200:
                ok = False; print(f'❌ V208/V322: طول بولت در {_blk["ver"]} بیش از ۲۰۰ نویسه است ({len(_b)}): {_b[:60]}…')
            if re.search(r'\bV\d', _b):
                ok = False; print(f'❌ V322: بولت در {_blk["ver"]} نباید شمارهٔ داخلی Vxxx داشته باشد: {_b}')
    if ok: print(f'✅ قالب CHANGELOG سالم است ({_top["ver"]}: {len(_top["bullets"])} تیتر، حداکثر {max(len(x) for x in _top["bullets"])} نویسه)')
# فایل‌های حذف‌شده که تستی به آن‌ها اشاره می‌کند
tests = subprocess.run(['bash', '-lc', 'grep -rhoE "(source|src)\\(\\"[^\\"]+\\"\\)" app/src/test | sort -u'], capture_output=True, text=True).stdout
for m in re.finditer(r'\("([^"]+)"\)', tests):
    path = m.group(1)
    if '$' in path or '/' not in path: continue  # قالب کاتلین یا نام نسبی کمکی (site/<name>.js)
    # V309 — تست‌های قدیمی با کمک src("…") از پوشهٔ app/src/main/java/ir/exam/app می‌خوانند
    if not (os.path.exists(path) or os.path.exists(os.path.join('app/src/main/java/ir/exam/app', path))): ok = False; print('❌ تست به فایل ناموجود اشاره می‌کند:', path)
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
# V232.4 — پین‌های غیرمتنی تست‌های سایت که check_test_pins نمی‌بیند (V218 regex، V182 اندازهٔ index.html)
_css = open('site/src/site.css', encoding='utf-8').read()
_w = len(re.findall(r'background:#fff\b', _css))
if _w: ok = False; print('❌ V218: background:#fff در site.css (از var(--card) یا #FFFFFF استفاده کن):', _w)
if os.path.isfile('site/index.html') and os.path.getsize('site/index.html') >= 1_000_000: ok = False; print('❌ V182: site/index.html بزرگ‌تر از سقف تست است')
print('\nPREFLIGHT:', 'PASS' if ok else 'FAIL'); sys.exit(0 if ok else 1)
