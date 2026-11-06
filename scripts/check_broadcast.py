#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""آزمون واقعی «پیام سازنده» روی PostgreSQL ۱۷ (ابزار محلی؛ در CI اجرا نمی‌شود).
اجرا:  python3 scripts/check_broadcast.py   (نیازمند postgresql نصب‌شده)

جدول‌ها و توابع از خود مهاجرت‌های V232/V284 ساخته می‌شوند، بعد فایل پخش اجرا می‌شود."""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path('/home/user/exam-app-kotlin')
MIG = ROOT / 'supabase/migrations'
PGBIN = sorted(pathlib.Path('/usr/lib/postgresql').glob('*/bin'))[-1]
DATA, SOCK = pathlib.Path('/tmp/bcpg'), '/tmp/bcsock'


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
sh(f'''{PGBIN}/pg_ctl -D {DATA} -o "-k {SOCK} -p 55433 -c listen_addresses=''" -l /tmp/bcpg.log start -w''')
PSQL = [str(PGBIN / 'psql'), '-h', SOCK, '-p', '55433', '-U', 'postgres', '-d', 'postgres', '-qtAX']
CURRENT = {'uid': None}


def sql(q):
    r = subprocess.run(PSQL + ['-c', q], capture_output=True, text=True)
    if r.returncode != 0:
        print('SQL ERROR:', r.stderr[:900]); sys.exit(1)
    return r.stdout.strip()


def sqlfile(p):
    r = subprocess.run([str(PGBIN / 'psql'), '-h', SOCK, '-p', '55433', '-U', 'postgres', '-d', 'postgres',
                        '-q', '-v', 'ON_ERROR_STOP=1', '-f', str(p)], capture_output=True, text=True)
    if r.returncode != 0:
        print('FILE ERROR:', p, r.stderr[:1200]); sys.exit(1)
    return r.stderr


v232 = (MIG / '20261009_native_push_notifications_v232.sql').read_text()
v284 = (MIG / '20261014_native_notification_center_v284.sql').read_text()


def block(text, start_pat, end=None):
    i = re.search(start_pat, text).start()
    body = text.index('as $$', i) + len('as $$')
    j = text.index('$$;', body) + 3
    return text[i:j]


tokens = re.search(r'create table if not exists public\.push_tokens\s*\(.*?\n\);', v232, re.S).group(0)
outbox = re.search(r'create table if not exists public\.push_outbox\s*\(.*?\n\);', v232, re.S).group(0)
tokens = tokens.replace('references auth.users(id) on delete cascade', '')
enqueue = block(v284, r'create or replace function private\.push_enqueue\(')
my_notif = block(v284, r'create or replace function public\.native_my_notifications_v284\(')
mark_read = block(v284, r'create or replace function public\.native_notifications_read_v284\(')

schema = """
create schema if not exists private;
create schema if not exists auth;
create table auth.users(id uuid primary key);
create table public.profiles(id uuid primary key, role text not null);
create or replace function auth.uid() returns uuid language sql stable as
$$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
""" + tokens + '\n' + outbox + """
alter table public.push_outbox add column if not exists read_at timestamptz;
create role authenticated;
""" + enqueue + '\n' + my_notif + '\n' + mark_read + """
grant execute on function public.native_my_notifications_v284(int) to authenticated;
grant execute on function public.native_notifications_read_v284(bigint[]) to authenticated;
"""
pathlib.Path('/tmp/bc_schema.sql').write_text(schema)
sqlfile('/tmp/bc_schema.sql')
print('schema built from the real V232 + V284 migrations\n')

# ۶ کاربر: ۲ با توکن اندروید، ۱ با توکن وب، ۳ بدون توکن
ids = ['%08d-0000-4000-8000-000000000000' % i for i in range(1, 7)]
for i, u in enumerate(ids):
    sql(f"insert into public.profiles values ('{u}', '{['teacher','manager','student'][i % 3]}')")
sql(f"insert into public.push_tokens(token,user_id,platform) values ('tok-a1','{ids[0]}','android'),('tok-a2','{ids[1]}','android'),('tok-w1','{ids[2]}','web')")

fails = []


def check(name, cond, extra=''):
    print(('  OK   ' if cond else '  FAIL ') + name + (('  → ' + str(extra)) if extra else ''))
    if not cond:
        fails.append(name)


bc = ROOT / 'sql/manual/SQL_BROADCAST_DEV_MESSAGE.sql'
v_body_text = re.search(r"v_body\s+text := '([^']*)'", bc.read_text()).group(1)
SLUG = re.search(r"v_slug\s+text := '([^']+)'", bc.read_text()).group(1)
err = sqlfile(bc)
check('فایل پخش بدون خطا اجرا شد', True)
m = re.search(r'برای (\d+) کاربر', err)
check('برای هر ۶ کاربر ثبت شد', m and m.group(1) == '6', err.strip().split('\n')[0] if err else '')

n = sql("select count(*) from public.push_outbox where kind='announcement'")
check('۶ ردیف در صندوق', n == '6', n)
n_tok = sql("select count(*) from public.push_outbox o join public.push_tokens t on t.user_id=o.user_id where o.kind='announcement'")
check('۳ نفر توکن دارند (اپ/وب)', n_tok == '3', n_tok)
n_notok = sql("select count(*) from public.push_outbox o where o.kind='announcement' and not exists(select 1 from public.push_tokens t where t.user_id=o.user_id)")
check('۳ نفر بدون توکن هم پیام را در مرکز اعلان می‌گیرند', n_notok == '3', n_notok)
t = sql("select distinct title from public.push_outbox where kind='announcement'")
check('عنوان «پیام سازنده» است', t == 'پیام سازنده', t)
check('همه در صف pending هستند', sql("select distinct state from public.push_outbox where kind='announcement'") == 'pending')

# اجرای دوباره = بدون تکرار
sqlfile(bc)
n2 = sql("select count(*) from public.push_outbox where kind='announcement'")
check('اجرای دوباره پیام تکراری نمی‌سازد', n2 == '6', n2)

# پیام بعدی با slug تازه
sql("update public.push_outbox set state='sent' where kind='announcement'")
p = pathlib.Path('/tmp/bc2.sql')
p.write_text(bc.read_text().replace(SLUG, SLUG + "-next").replace(v_body_text, "پیام دوم"))
sqlfile(p)
n3 = sql("select count(*) from public.push_outbox where kind='announcement'")
check('پیام دوم با شناسهٔ تازه ساخته می‌شود', n3 == '12', n3)

# مرکز اعلان کاربر بدون توکن
sql(f"set request.jwt.claim.sub = '{ids[5]}'")
r = subprocess.run(PSQL + ['-c', f"set request.jwt.claim.sub = '{ids[5]}'; select public.native_my_notifications_v284(50)::text"],
                   capture_output=True, text=True)
js = r.stdout.strip()
check('مرکز اعلان پیام را نشان می‌دهد', 'پیام سازنده' in js, js[:90] + '…')
check('kind آن announcement است', '"kind": "announcement"' in js)
check('شمارندهٔ خوانده‌نشده درست است', '"unread": 2' in js, re.search(r'"unread": \d+', js).group(0) if '"unread"' in js else '')

# بریده‌شدن متن بلند
long_body = 'x' * 600
p2 = pathlib.Path('/tmp/bc3.sql')
s = bc.read_text().replace(SLUG, SLUG + '-long')
s = re.sub(r"v_body  text := '[^']*';", "v_body  text := '" + long_body + "';", s)
p2.write_text(s)
sqlfile(p2)
ln = sql(f"select max(length(body)) from public.push_outbox where dedupe_key like 'broadcast:{SLUG}-long:%'")
check('متن بلند به ۴۰۰ نویسه بریده می‌شود', ln == '400', ln)

# لغو پیام ارسال‌نشده
sql(f"delete from public.push_outbox where kind='announcement' and dedupe_key like 'broadcast:{SLUG}-long:%' and state='pending'")
left = sql(f"select count(*) from public.push_outbox where dedupe_key like 'broadcast:{SLUG}-long:%'")
check('لغو پیام ارسال‌نشده کار می‌کند', left == '0', left)

subprocess.run(f'{PGBIN}/pg_ctl -D {DATA} stop -m immediate', shell=True, capture_output=True)
print()
for f in fails:
    print('  x', f)
print('BROADCAST_DB_TEST:', 'FAIL (%d)' % len(fails) if fails else 'PASS')
sys.exit(1 if fails else 0)
