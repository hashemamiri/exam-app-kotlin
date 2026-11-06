-- =====================================================================
-- کمپین «اولین شارژ دوبرابر» — کنترل و گزارش
--
-- از V345 به بعد هدیه خودکار است. برای نصب/راه‌اندازی این فایل را اجرا نکنید؛
-- فایل نصب این است:
--   supabase/migrations/20261106_wallet_first_charge_bonus_v345.sql
--
-- این فایل فقط برای «کنترل کمپین» و «دیدن گزارش» است. هر بخش را جداگانه
-- انتخاب و Run کنید. هیچ بخشی به‌تنهایی هدیه‌ای پرداخت نمی‌کند.
--
-- قاعدهٔ فعال: نخستین شارژ موفقِ تاریخِ حساب، مشروط بر این‌که مبلغ شارژ
-- از ۱٬۰۰۰٬۰۰۰ تومان بیشتر نباشد؛ هدیه = عین مبلغ شارژ.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ۱) تنظیمات فعلی
-- ---------------------------------------------------------------------
select enabled              as روشن,
       max_charge_toman     as سقف_مبلغ_شارژ,
       starts_at            as آغاز,
       ends_at              as پایان,
       notify               as اعلان_هدیه
from private.wallet_bonus_config where id = 1;

-- ---------------------------------------------------------------------
-- ۲) خلاصهٔ عملکرد
-- ---------------------------------------------------------------------
select case status
         when 'granted' then 'هدیه داده شد'
         when 'partial' then 'هدیهٔ ناقص (سقف کیف پول)'
         when 'skipped' then 'رد شد'
         when 'error'   then 'خطا'
       end                as وضعیت,
       count(*)           as تعداد,
       sum(bonus_toman)   as جمع_هدیه_تومان
from public.wallet_bonus_log
group by status order by status;

-- ---------------------------------------------------------------------
-- ۳) چه کسانی هدیه گرفتند
-- ---------------------------------------------------------------------
select coalesce(nullif(p.display_name, ''), p.full_name, l.user_id::text) as کاربر,
       l.charge_toman as مبلغ_شارژ,
       l.bonus_toman  as هدیه,
       l.status       as وضعیت,
       l.created_at   as زمان
from public.wallet_bonus_log l
left join public.profiles p on p.id = l.user_id
where l.status in ('granted', 'partial')
order by l.created_at desc
limit 100;

-- ---------------------------------------------------------------------
-- ۴) چه کسانی رد شدند و چرا
-- ---------------------------------------------------------------------
select coalesce(note, status) as دلیل, count(*) as تعداد
from public.wallet_bonus_log
where status in ('skipped', 'error')
group by coalesce(note, status)
order by تعداد desc;

-- ---------------------------------------------------------------------
-- ۵) آیا چیزی از قلم افتاده؟ (باید خالی باشد)
-- شارژهای موفقِ مشمولی که هیچ رکورد هدیه ندارند.
-- ---------------------------------------------------------------------
select o.id as سفارش, o.user_id, o.amount_toman as مبلغ, o.paid_at
from public.wallet_payment_orders o, private.wallet_bonus_config c
where c.id = 1 and o.status = 'paid'
  and o.paid_at >= c.starts_at and (c.ends_at is null or o.paid_at < c.ends_at)
  and o.amount_toman <= c.max_charge_toman
  and not exists (select 1 from public.wallet_bonus_log l where l.order_id = o.id)
order by o.paid_at desc
limit 50;

-- =====================================================================
-- ۶) کلیدهای کنترل — فقط خط مورد نظر را انتخاب و Run کنید
-- =====================================================================
-- خاموش کردن فوری کمپین (شارژها طبق روال انجام می‌شوند، فقط هدیه قطع می‌شود):
--   update private.wallet_bonus_config set enabled = false where id = 1;
--
-- روشن کردن دوباره:
--   update private.wallet_bonus_config set enabled = true where id = 1;
--
-- تعیین تاریخ پایان:
--   update private.wallet_bonus_config set ends_at = '2026-12-01 00:00:00+03:30' where id = 1;
--
-- برداشتن تاریخ پایان (بی‌پایان):
--   update private.wallet_bonus_config set ends_at = null where id = 1;
--
-- تغییر سقف مبلغ شارژ:
--   update private.wallet_bonus_config set max_charge_toman = 500000 where id = 1;
--
-- خاموش کردن اعلان «هدیهٔ اولین شارژ» (هدیه همچنان اعمال می‌شود):
--   update private.wallet_bonus_config set notify = false where id = 1;

-- =====================================================================
-- ۷) جبران گذشته — اگر کاربری پیش از نصب V345 شارژ کرده و مشمول بوده
-- این بخش را فقط در صورت نیاز و یک بار اجرا کنید.
-- =====================================================================
-- do $$
-- declare r record; v_res text; n int := 0;
-- begin
--   for r in
--     select o.id from public.wallet_payment_orders o, private.wallet_bonus_config c
--     where c.id = 1 and o.status = 'paid'
--       and o.paid_at >= c.starts_at and (c.ends_at is null or o.paid_at < c.ends_at)
--       and o.amount_toman <= c.max_charge_toman
--       and not exists (select 1 from public.wallet_bonus_log l where l.order_id = o.id)
--     order by o.paid_at
--   loop
--     v_res := private.wallet_grant_first_charge_bonus(r.id);
--     if v_res in ('granted','partial') then n := n + 1; end if;
--   end loop;
--   raise notice 'جبران انجام شد: % هدیه', n;
-- end $$;

-- =====================================================================
-- ۸) بازگرداندن یک هدیهٔ مشخص (اگر اشتباهی اعمال شد)
-- جای <شناسه کاربر> را با uuid کاربر عوض کنید.
-- =====================================================================
-- do $$
-- declare r record; v_bal bigint;
-- begin
--   for r in select * from public.wallet_tx
--            where reason = 'gift:first_charge_bonus' and user_id = '<شناسه کاربر>'::uuid
--            order by id desc loop
--     select balance into v_bal from public.wallets where user_id = r.user_id for update;
--     if v_bal < r.amount then
--       raise notice 'کاربر اعتبار را خرج کرده؛ دستی بررسی شود'; continue;
--     end if;
--     update public.wallets set balance = balance - r.amount, updated_at = now()
--      where user_id = r.user_id returning balance into v_bal;
--     insert into public.wallet_tx(user_id, amount, reason, balance_after)
--     values (r.user_id, -r.amount, 'gift_revoke:first_charge_bonus', v_bal);
--     delete from public.wallet_tx where id = r.id;
--     delete from public.wallet_bonus_log where order_id = (
--       select order_id from public.wallet_bonus_log where user_id = r.user_id limit 1);
--   end loop;
-- end $$;
-- =====================================================================
