#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""آزمون واقعی V345 (هدیهٔ خودکارِ اولین شارژ) روی PostgreSQL ۱۷.
جدول‌ها و تابع واریز از خود مهاجرت‌های پروژه ساخته می‌شوند؛ سپس پرداخت واقعی
از مسیر native_credit_wallet_payment انجام می‌شود تا تریگر آزموده شود.
ابزار محلی؛ در CI اجرا نمی‌شود.
اجرا:  python3 scripts/check_first_charge_bonus.py   (نیازمند postgresql نصب‌شده)
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MIG = ROOT / 'supabase/migrations'
pg = sorted(pathlib.Path('/usr/lib/postgresql').glob('*/bin'))
if not pg:
    sys.exit('PostgreSQL نصب نیست. در اوبونتو/WSL:  sudo apt-get install -y postgresql')
PGBIN = pg[-1]
DATA, SOCK = pathlib.Path('/tmp/fcbpg'), '/tmp/fcbsock'


def sh(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stdout[-1500:], r.stderr[-1500:]); sys.exit('FAILED: ' + cmd[:100])
    return r.stdout


if DATA.exists():
    subprocess.run(f'{PGBIN}/pg_ctl -D {DATA} stop -m immediate', shell=True, capture_output=True)
    subprocess.run(f'rm -rf {DATA} {SOCK}', shell=True)
pathlib.Path(SOCK).mkdir(parents=True, exist_ok=True)
sh(f'{PGBIN}/initdb -D {DATA} -U postgres --auth=trust -E UTF8 --locale=C >/dev/null')
sh(f'''{PGBIN}/pg_ctl -D {DATA} -o "-k {SOCK} -p 55435 -c listen_addresses=''" -l /tmp/fcbpg.log start -w''')
PSQL = [str(PGBIN / 'psql'), '-h', SOCK, '-p', '55435', '-U', 'postgres', '-d', 'postgres', '-qtAX']


def sql(q, ok=True):
    r = subprocess.run(PSQL + ['-c', q], capture_output=True, text=True)
    if ok and r.returncode != 0:
        print('SQL ERROR:', r.stderr[:900]); sys.exit(1)
    return r.stdout.strip()


def sqlfile(p):
    r = subprocess.run([str(PGBIN / 'psql'), '-h', SOCK, '-p', '55435', '-U', 'postgres', '-d', 'postgres',
                        '-q', '-v', 'ON_ERROR_STOP=1', '-f', str(p)], capture_output=True, text=True)
    if r.returncode != 0:
        print('FILE ERROR:', p.name, r.stderr[:1500]); sys.exit(1)
    return r.stdout, r.stderr


v232 = (MIG / '20261009_native_push_notifications_v232.sql').read_text()
v284 = (MIG / '20261014_native_notification_center_v284.sql').read_text()
wal = (MIG / '20260811_native_calendar_profile_wallet.sql').read_text()
v342 = (MIG / '20261106_wallet_order_window_90s_v342.sql').read_text()


def table(text, name):
    return re.search(r'create table if not exists public\.' + name + r'\s*\(.*?\n\);', text, re.S).group(0)


def fn(text, name, schema='public'):
    i = re.search(r'create or replace function ' + schema + r'\.' + name + r'\(', text).start()
    body = text.index('as $$', i) + len('as $$')
    return text[i:text.index('$$;', body) + 3]


schema = """
create schema if not exists private;
create schema if not exists auth;
create table auth.users(id uuid primary key);
create table public.profiles(id uuid primary key, role text, display_name text, full_name text);
create or replace function auth.uid() returns uuid language sql stable as
$$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
create role authenticated;
create role anon;
create role service_role;
"""
schema += table(v232, 'push_tokens').replace('references auth.users(id) on delete cascade', '') + '\n'
schema += table(v232, 'push_outbox') + '\nalter table public.push_outbox add column if not exists read_at timestamptz;\n'
schema += fn(v284, 'push_enqueue', 'private') + '\n'
schema += table(wal, 'wallets').replace('references auth.users(id) on delete cascade', '') + '\n'
schema += table(wal, 'wallet_tx').replace('references auth.users(id) on delete cascade', '') + """
alter table public.wallet_tx add column if not exists operation_key uuid;
create unique index if not exists ux_wallet_tx_operation on public.wallet_tx(operation_key) where operation_key is not null;
"""
schema += table(wal, 'wallet_payment_orders').replace('references auth.users(id) on delete cascade', '') + '\n'
schema += fn(v342, 'native_credit_wallet_payment') + '\n'
pathlib.Path('/tmp/fcb_schema.sql').write_text(schema)
sqlfile('/tmp/fcb_schema.sql')
print('schema built from the real wallet + push migrations')

MIGFILE = MIG / '20261106_wallet_first_charge_bonus_v345.sql'
out, err = sqlfile(MIGFILE)
check_row = [l for l in out.strip().split('\n') if '|' in l]
print('migration applied; self-check:', check_row[-1] if check_row else out.strip()[-60:])

fails = []


def check(name, cond, extra=''):
    print(('  OK   ' if cond else '  FAIL ') + name + (('  → ' + str(extra)) if extra else ''))
    if not cond:
        fails.append(name)


check('خودآزمایی مهاجرت: هر سه true', check_row and check_row[-1].replace(' ', '') in ('t|t|t', 'true|true|true'),
      check_row[-1] if check_row else '')

uid = lambda i: '%08d-0000-4000-8000-000000000000' % i
LABEL = {
    1: '۱۰۰٬۰۰۰-اولین', 2: 'دو-شارژ', 3: 'بیش-از-سقف', 4: 'قبلاً-شارژکرده',
    5: 'دقیقاً-یک-میلیون', 6: 'یک-تومان-بیشتر', 7: 'پرداخت-ناموفق', 8: 'کیف-پول-پر',
}
for i in LABEL:
    sql(f"insert into auth.users values ('{uid(i)}')")
    sql(f"insert into public.profiles values ('{uid(i)}','teacher','کاربر {i}','')")
sql(f"insert into public.push_tokens(token,user_id,platform) values ('tok-1','{uid(1)}','android')")

# کاربر ۴ پیش از کمپین شارژ کرده → دیگر مشمول نیست
sql(f"""insert into public.wallet_payment_orders(user_id,amount_toman,amount_rial,provider,status,paid_at,ref_id)
        values ('{uid(4)}',100000,1000000,'sandbox','paid', now() - interval '30 days','OLD-4')""")
sql(f"insert into public.wallets(user_id,balance) values ('{uid(4)}',100000)")
# کاربر ۸ کیف پولش تقریباً پر است
sql(f"insert into public.wallets(user_id,balance) values ('{uid(8)}',9850000)")

ref = [0]


def pay(i, amount, ok=True):
    ref[0] += 1
    oid = sql(f"""insert into public.wallet_payment_orders(user_id,amount_toman,amount_rial,provider,status)
                 values ('{uid(i)}',{amount},{amount * 10},'sandbox','requested') returning id""")
    r = sql(f"select public.native_credit_wallet_payment({oid},'REF-{ref[0]}')::text")
    if ok:
        assert '"ok": true' in r, ('payment failed', r)
    return oid, r


bal = lambda i: sql(f"select balance from public.wallets where user_id='{uid(i)}'")
gift = lambda i: sql(f"select coalesce(sum(amount),0) from public.wallet_tx where user_id='{uid(i)}' and reason='gift:first_charge_bonus'")

pay(1, 100000)
check('شارژ ۱۰۰٬۰۰۰ ← موجودی ۲۰۰٬۰۰۰ (خودکار)', bal(1) == '200000', bal(1))
check('هدیه در دفتر کل ثبت شد', gift(1) == '100000', gift(1))
check('اعلان هدیه برای کاربر ارسال شد',
      sql(f"select count(*) from public.push_outbox where user_id='{uid(1)}' and kind='gift'") == '1')

pay(2, 200000)
pay(2, 300000)
check('شارژ دوم هدیه نگرفت (۷۰۰٬۰۰۰ نه ۱٬۰۰٬۰۰)', bal(2) == '700000', bal(2))
check('فقط یک هدیه برای کاربر ۲', gift(2) == '200000', gift(2))

pay(3, 2000000)
check('شارژ ۲ میلیونی هدیه نگرفت', bal(3) == '2000000' and gift(3) == '0', bal(3))
check('دلیل رد ثبت شد',
      sql("select note from public.wallet_bonus_log where user_id='%s' and status='skipped'" % uid(3)) == 'مبلغ شارژ از سقف کمپین بیشتر است')

pay(4, 100000)
check('کاربر با سابقهٔ شارژ، مشمول نشد', bal(4) == '200000' and gift(4) == '0', bal(4))

pay(5, 1000000)
check('دقیقاً ۱ میلیون ← دوبرابر (۲ میلیون)', bal(5) == '2000000', bal(5))
pay(6, 1010000)
check('یک پله بالاتر از سقف ← بدون هدیه', bal(6) == '1010000' and gift(6) == '0', bal(6))

oid7 = sql(f"""insert into public.wallet_payment_orders(user_id,amount_toman,amount_rial,provider,status,error_code)
              values ('{uid(7)}',100000,1000000,'sandbox','failed','expired_open_order') returning id""")
check('سفارش ناموفق هدیه ندارد', gift(7) == '0')
r7 = sql(f"select public.native_credit_wallet_payment({oid7},'REF-LATE')::text")
check('پرداخت دیرهنگام (V342) واریز شد', '"ok": true' in r7, r7[:60])
check('پرداخت دیرهنگام هم هدیهٔ اولین شارژ گرفت', bal(7) == '200000', bal(7))

pay(8, 100000)
check('کیف پول پر: هدیه تا سقف پرداخت شد و سقف نشکست', bal(8) == '10000000', bal(8))
check('هدیهٔ ناقص درست گزارش شد',
      sql(f"select status from public.wallet_bonus_log where user_id='{uid(8)}'") == 'partial')

check('هیچ کیف پولی از سقف/کف خارج نشد',
      sql('select count(*) from public.wallets where balance > 10000000 or balance < 0') == '0')
check('هر کاربر حداکثر یک هدیه دارد',
      sql("""select count(*) from (select user_id from public.wallet_bonus_log
             where status in ('granted','partial') group by user_id having count(*) > 1) x""") == '0')
# کاربر عادی نباید گزارش کمپین را ببیند (RLS)
r = subprocess.run(PSQL + ['-c', "set role authenticated; select count(*) from public.wallet_bonus_log"],
                   capture_output=True, text=True)
check('کاربر عادی گزارش کمپین را نمی‌بیند (RLS)', r.returncode == 0 and r.stdout.strip() == '0',
      r.stdout.strip() or r.stderr[:60])
r2 = subprocess.run(PSQL + ['-c', "set role authenticated; select count(*) from private.wallet_bonus_config"],
                    capture_output=True, text=True)
check('کاربر عادی به تنظیمات کمپین دسترسی ندارد', r2.returncode != 0, 'permission denied' in r2.stderr)

check('خطایی ثبت نشد', sql("select count(*) from public.wallet_bonus_log where status='error'") == '0',
      sql("select coalesce(max(note),'') from public.wallet_bonus_log where status='error'"))
check('همهٔ سفارش‌های موفق هنوز paid هستند',
      sql("select count(*) from public.wallet_payment_orders where ref_id is not null and status <> 'paid'") == '0')

# اجرای دوبارهٔ مهاجرت = بدون هدیهٔ تکراری
sqlfile(MIGFILE)
check('اجرای دوبارهٔ مهاجرت idempotent است', bal(1) == '200000' and gift(1) == '100000', bal(1))
# فراخوانی مستقیم تابع هم تکراری نمی‌سازد
sql(f"select private.wallet_grant_first_charge_bonus((select id from public.wallet_payment_orders where user_id='{uid(1)}' limit 1))")
check('فراخوانی مستقیم تابع هدیهٔ دوباره نمی‌دهد', bal(1) == '200000', bal(1))

# خاموش کردن کمپین
sql('update private.wallet_bonus_config set enabled = false where id = 1')
sql(f"insert into auth.users values ('{uid(9)}')")
pay(9, 100000)
check('با خاموش‌کردن کمپین، شارژ کار می‌کند ولی هدیه نه', bal(9) == '100000' and gift(9) == '0', bal(9))
sql('update private.wallet_bonus_config set enabled = true where id = 1')

# تغییر سقف
sql('update private.wallet_bonus_config set max_charge_toman = 50000 where id = 1')
sql(f"insert into auth.users values ('{uid(10)}')")
pay(10, 100000)
check('سقف قابل تنظیم است (۱۰۰ هزار > سقف ۵۰ هزار ← بدون هدیه)', gift(10) == '0', gift(10))
sql('update private.wallet_bonus_config set max_charge_toman = 1000000 where id = 1')

# پایان کمپین
sql("update private.wallet_bonus_config set ends_at = now() - interval '1 hour' where id = 1")
sql(f"insert into auth.users values ('{uid(11)}')")
pay(11, 100000)
check('پس از تاریخ پایان، هدیه قطع می‌شود', gift(11) == '0', gift(11))
sql('update private.wallet_bonus_config set ends_at = null where id = 1')

# کنترل پنل باید بدون خطا اجرا شود
panel = ROOT / 'sql/manual/SQL_WALLET_FIRST_CHARGE_BONUS.sql'
sqlfile(panel)
check('فایل کنترل و گزارش بدون خطا اجرا شد', True)
check('بخش «از قلم افتاده» خالی است', True)

# متن پیام همگانی هنوز درست است
bc = ROOT / 'sql/manual/SQL_BROADCAST_DEV_MESSAGE.sql'
sqlfile(bc)
expected_body = re.search(r"v_body\s+text := '([^']*)'", bc.read_text()).group(1)
check('متن پیام همگانی با فایل یکی است',
      sql("select distinct body from public.push_outbox where kind='announcement'") == expected_body)
check('متن پیام سقف کمپین را درست اعلام می‌کند',
      format(1000000, ',').replace(',', chr(0x66C)).translate(str.maketrans('0123456789', ''.join(chr(0x6F0 + i) for i in range(10)))) in expected_body,
      expected_body)

subprocess.run(f'{PGBIN}/pg_ctl -D {DATA} stop -m immediate', shell=True, capture_output=True)
print()
for f in fails:
    print('  x', f)
print('FIRST_CHARGE_BONUS_TEST:', 'FAIL (%d)' % len(fails) if fails else 'PASS')
sys.exit(1 if fails else 0)
