-- =====================================================================
-- V345 — کمپین «اولین شارژ دوبرابر» به‌صورت خودکار
--
-- قاعده:
--   • فقط نخستین شارژ موفقِ تاریخِ حساب کاربر (کاربرانی که قبلاً شارژ
--     کرده‌اند مشمول نیستند).
--   • فقط اگر مبلغ شارژ <= ۱٬۰۰۰٬۰۰۰ تومان باشد. شارژ بالاتر هدیه ندارد.
--   • هدیه = عین مبلغ شارژ (یعنی اعتبار دوبرابر می‌شود).
--   • خودکار: به‌محض تأیید پرداخت اعمال می‌شود؛ هیچ اجرای دستی لازم نیست.
--
-- ایمنی:
--   • اگر هدیه به هر دلیلی خطا بدهد، پرداخت کاربر هرگز خراب نمی‌شود.
--   • هدیهٔ تکراری ممکن نیست (کلید یکتا برای هر سفارش + یک هدیه به ازای هر کاربر).
--   • سقف ۱۰٬۰۰۰٬۰۰۰ تومان کیف پول نقض نمی‌شود.
--   • همه‌چیز در جدول گزارش wallet_bonus_log ثبت می‌شود.
--   • تابع واریز V342 (پنجرهٔ ۹۰ ثانیه) دست‌نخورده می‌ماند.
--
-- اجرا: یک بار در Supabase → SQL Editor → New query → Run.
-- پیش‌نیاز: SQL کیف پول (20260811) و اعلان‌ها (V232/V284).
-- =====================================================================
begin;

create schema if not exists private;

-- ---------------------------------------------------------------------
-- تنظیمات کمپین (بعداً بدون اجرای دوبارهٔ این فایل قابل تغییر است)
-- ---------------------------------------------------------------------
create table if not exists private.wallet_bonus_config(
  id int primary key default 1 check(id = 1),
  enabled boolean not null default true,
  max_charge_toman bigint not null default 1000000,
  starts_at timestamptz not null default now(),
  ends_at timestamptz,
  notify boolean not null default true
);
insert into private.wallet_bonus_config(id, enabled, max_charge_toman, starts_at, ends_at, notify)
values (1, true, 1000000, now(), null, true)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------
-- گزارش کمپین: هر تصمیمی ثبت می‌شود (اعمال / رد / خطا)
-- ---------------------------------------------------------------------
create table if not exists public.wallet_bonus_log(
  id bigserial primary key,
  order_id bigint not null,
  user_id uuid not null,
  charge_toman bigint not null,
  bonus_toman bigint not null default 0,
  status text not null check(status in ('granted','partial','skipped','error')),
  note text,
  created_at timestamptz not null default now()
);
-- هر کاربر حداکثر یک هدیه؛ حتی در پرداخت‌های هم‌زمان
create unique index if not exists ux_wallet_bonus_one_per_user
  on public.wallet_bonus_log(user_id) where status in ('granted','partial');
create unique index if not exists ux_wallet_bonus_order
  on public.wallet_bonus_log(order_id) where status in ('granted','partial');
create index if not exists idx_wallet_bonus_log_created on public.wallet_bonus_log(created_at desc);
alter table public.wallet_bonus_log enable row level security;
-- هیچ policy مستقیم: RLS همهٔ ردیف‌ها را از کاربر عادی پنهان می‌کند.
-- گرنت صریح طبق قرارداد V324 لازم است تا Data API خطای permission denied ندهد.
grant select on table public.wallet_bonus_log to anon;
grant select on table public.wallet_bonus_log to authenticated;
grant all on table public.wallet_bonus_log to service_role;

-- ---------------------------------------------------------------------
-- منطق هدیه (یک‌جا؛ هم تریگر و هم جبران گذشته از همین استفاده می‌کنند)
-- ---------------------------------------------------------------------
create or replace function private.wallet_grant_first_charge_bonus(p_order bigint)
returns text
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  c record;
  o record;
  v_prior int;
  v_bonus bigint;
  v_balance bigint;
  v_room bigint;
  v_key uuid;
  v_status text := 'granted';
  v_note text := null;
  v_inserted int;
begin
  select * into c from private.wallet_bonus_config where id = 1;
  if not found or not c.enabled then return 'disabled'; end if;

  select * into o from public.wallet_payment_orders where id = p_order;
  if not found or o.status <> 'paid' then return 'not_paid'; end if;

  v_key := md5('first_charge_bonus:' || o.id::text)::uuid;
  if exists (select 1 from public.wallet_tx where operation_key = v_key) then
    return 'already';
  end if;

  if o.paid_at is null or o.paid_at < c.starts_at
     or (c.ends_at is not null and o.paid_at >= c.ends_at) then
    return 'outside_window';
  end if;

  if exists (select 1 from public.wallet_bonus_log where order_id = o.id) then
    return 'already';
  end if;

  if o.amount_toman > c.max_charge_toman then
    insert into public.wallet_bonus_log(order_id, user_id, charge_toman, status, note)
    values (o.id, o.user_id, o.amount_toman, 'skipped', 'مبلغ شارژ از سقف کمپین بیشتر است');
    return 'over_limit';
  end if;

  -- نخستین شارژ موفقِ تاریخِ حساب؟
  select count(*) into v_prior
  from public.wallet_payment_orders
  where user_id = o.user_id and status = 'paid' and id <> o.id;
  if v_prior > 0 then
    insert into public.wallet_bonus_log(order_id, user_id, charge_toman, status, note)
    values (o.id, o.user_id, o.amount_toman, 'skipped', 'اولین شارژ حساب نیست');
    return 'not_first';
  end if;

  v_bonus := o.amount_toman;

  -- این درج، قفلِ «یک هدیه به ازای هر کاربر» است
  insert into public.wallet_bonus_log(order_id, user_id, charge_toman, bonus_toman, status)
  values (o.id, o.user_id, o.amount_toman, v_bonus, 'granted')
  on conflict do nothing;
  get diagnostics v_inserted = row_count;
  if v_inserted = 0 then return 'already'; end if;

  insert into public.wallets(user_id, balance) values (o.user_id, 0)
  on conflict (user_id) do nothing;
  select balance into v_balance from public.wallets where user_id = o.user_id for update;

  v_room := 10000000 - v_balance;
  if v_room <= 0 then
    update public.wallet_bonus_log set status = 'skipped', bonus_toman = 0,
           note = 'کیف پول پر است؛ نیازمند بررسی پشتیبانی'
     where order_id = o.id and status = 'granted';
    return 'wallet_full';
  end if;
  if v_bonus > v_room then
    v_bonus := v_room;
    v_status := 'partial';
    v_note := 'هدیه تا سقف کیف پول پرداخت شد';
  end if;

  update public.wallets
     set balance = balance + v_bonus, updated_at = now()
   where user_id = o.user_id
  returning balance into v_balance;

  insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
  values (o.user_id, v_bonus, 'gift:first_charge_bonus', v_balance, v_key);

  update public.wallet_bonus_log
     set bonus_toman = v_bonus, status = v_status, note = v_note
   where order_id = o.id and status = 'granted';

  if c.notify then
    perform private.push_enqueue(
      o.user_id, 'gift', 'هدیهٔ اولین شارژ',
      'مبلغ ' || to_char(v_bonus, 'FM999,999,999') || ' تومان اعتبار هدیه به کیف پول شما اضافه شد.',
      jsonb_build_object('page', ''), 'giftbonus:' || o.id::text);
  end if;

  return v_status;

exception when others then
  -- پرداخت کاربر هرگز نباید به‌خاطر هدیه برگشت بخورد
  begin
    insert into public.wallet_bonus_log(order_id, user_id, charge_toman, status, note)
    values (p_order, coalesce(o.user_id, '00000000-0000-0000-0000-000000000000'::uuid),
            coalesce(o.amount_toman, 0), 'error', left(sqlerrm, 300));
  exception when others then null;
  end;
  return 'error';
end $$;

-- ---------------------------------------------------------------------
-- تریگر: به‌محض این‌که سفارش «paid» شد
-- ---------------------------------------------------------------------
create or replace function private.wallet_first_charge_bonus_trigger()
returns trigger language plpgsql security definer set search_path = public, pg_temp
as $$
begin
  if new.status = 'paid' and coalesce(old.status, '') <> 'paid' then
    perform private.wallet_grant_first_charge_bonus(new.id);
  end if;
  return new;
end $$;

drop trigger if exists wallet_first_charge_bonus on public.wallet_payment_orders;
create trigger wallet_first_charge_bonus
  after update of status on public.wallet_payment_orders
  for each row execute function private.wallet_first_charge_bonus_trigger();

revoke all on function private.wallet_grant_first_charge_bonus(bigint) from public, anon, authenticated;
revoke all on function private.wallet_first_charge_bonus_trigger() from public, anon, authenticated;

commit;

-- =====================================================================
-- کنترل کمپین (هر وقت خواستید، جداگانه اجرا کنید)
-- =====================================================================
-- خاموش کردن فوری:
--   update private.wallet_bonus_config set enabled = false where id = 1;
-- روشن کردن:
--   update private.wallet_bonus_config set enabled = true where id = 1;
-- پایان دادن در تاریخ مشخص:
--   update private.wallet_bonus_config set ends_at = '2026-12-01 00:00:00+03:30' where id = 1;
-- تغییر سقف مبلغ شارژ:
--   update private.wallet_bonus_config set max_charge_toman = 500000 where id = 1;
-- خاموش کردن اعلان هدیه (هدیه همچنان اعمال می‌شود):
--   update private.wallet_bonus_config set notify = false where id = 1;
-- دیدن تنظیمات فعلی:
--   select * from private.wallet_bonus_config where id = 1;
--
-- =====================================================================
-- گزارش
-- =====================================================================
-- خلاصه:
--   select status, count(*) as تعداد, sum(bonus_toman) as جمع_هدیه
--   from public.wallet_bonus_log group by status order by status;
--
-- چه کسانی هدیه گرفتند:
--   select coalesce(nullif(p.display_name,''), p.full_name, l.user_id::text) as کاربر,
--          l.charge_toman as شارژ, l.bonus_toman as هدیه, l.status, l.created_at
--   from public.wallet_bonus_log l left join public.profiles p on p.id = l.user_id
--   where l.status in ('granted','partial') order by l.created_at desc limit 100;
--
-- چه کسانی رد شدند و چرا:
--   select note as دلیل, count(*) from public.wallet_bonus_log
--   where status in ('skipped','error') group by note order by 2 desc;
--
-- =====================================================================
-- بررسی پس از اجرا (هر سه ستون باید true باشند)
-- =====================================================================
select
  exists(select 1 from pg_trigger t join pg_class r on r.oid = t.tgrelid
         where r.relname = 'wallet_payment_orders' and t.tgname = 'wallet_first_charge_bonus'
           and not t.tgisinternal)                                as trigger_installed,
  exists(select 1 from private.wallet_bonus_config
         where id = 1 and enabled and max_charge_toman = 1000000)   as config_ready,
  exists(select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
         where n.nspname = 'private' and p.proname = 'wallet_grant_first_charge_bonus') as function_ready;
