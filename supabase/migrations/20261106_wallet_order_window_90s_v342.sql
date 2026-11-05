-- =====================================================================

-- V342 — پنجرهٔ سفارش پرداخت باز: ۹۰ ثانیه + پیام شمارش معکوس
--
-- درخواست کاربر:
--   «سفارش پرداخت مدیر یا معلم پس از 90 ثانیه منقضی شود و بتواند سفارش جدید ایجاد کند.»
--   پیام: «یک سفارش پرداخت باز دارید؛ ابتدا همان را تکمیل یا لغو کنید یا 90 ثانیه صبر کنید»
--   و اگر ۴۰ ثانیه گذشته باشد: «… یا 50 ثانیه صبر کنید» (یعنی زمانِ باقی‌مانده).
--
-- تصمیم کاربر دربارهٔ پرداخت دیرهنگام (ask_user):
--   سفارش بعد از ۹۰ ثانیه آزاد می‌شود، ولی اگر کاربر پرداخت قبلی را بعداً در بانک کامل کند،
--   مبلغش همچنان به کیف پول می‌نشیند. پس native_credit_wallet_payment هم اصلاح می‌شود تا
--   سفارشِ «فقط برای آزادسازیِ رابط منقضی‌شده» را بپذیرد. بدون این، با پنجرهٔ ۹۰ ثانیه
--   پول کاربر کم می‌شد ولی کیف پول شارژ نمی‌شد.
--
-- امنیت: مثل قبل security definer با search_path ثابت و grant فقط به service_role.
-- هیچ داده‌ای تغییر نمی‌کند؛ تکرار این فایل بی‌خطر است.
--
-- اجرا: Supabase → SQL Editor (یک‌بار).

-- ------------------------------------------------------------------ ۱) ساخت سفارش
create or replace function public.native_create_wallet_payment_order(
    p_user uuid,
    p_amount_toman bigint,
    p_provider text
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_balance bigint;
    v_id bigint;
    v_operation uuid;
    v_created timestamptz;
    v_wait int;
begin
    -- V341 — کادر مدرسه: معلم و مدیر هر دو کیف پول دارند.
    if p_user is null or not exists (
        select 1 from public.profiles where id = p_user and role in ('teacher', 'manager')
    ) then return jsonb_build_object('error', 'حساب کادر مدرسه معتبر نیست'); end if;

    if p_amount_toman < 100000 or p_amount_toman > 10000000 or p_amount_toman % 10000 <> 0 then
        return jsonb_build_object('error', 'مبلغ شارژ نامعتبر است');
    end if;
    if p_provider not in ('zarinpal', 'idpay', 'sandbox', 'zibal') then
        return jsonb_build_object('error', 'درگاه نامعتبر است');
    end if;

    insert into public.wallets(user_id, balance) values (p_user, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = p_user for update;
    if v_balance + p_amount_toman > 10000000 then
        return jsonb_build_object('error', 'موجودی پس از شارژ از سقف مجاز بیشتر می‌شود');
    end if;

    -- V342 — سفارش باز پس از ۹۰ ثانیه آزاد می‌شود (پیش‌تر ۳۰ دقیقه بود).
    -- error_code مخصوصِ 'expired_open_order' می‌ماند تا اگر پرداختِ همین سفارش بعداً
    -- در بانک موفق شد، واریز همچنان انجام شود (بخش ۲ همین فایل).
    update public.wallet_payment_orders
    set status = 'failed', error_code = 'expired_open_order'
    where user_id = p_user
      and status in ('pending', 'requested', 'verifying')
      and created_at < now() - interval '90 seconds';

    -- V342 — زمانِ باقی‌مانده تا آزاد شدن سفارش، در پیام به کاربر گفته می‌شود.
    select created_at into v_created
    from public.wallet_payment_orders
    where user_id = p_user and status in ('pending', 'requested', 'verifying')
    order by created_at desc
    limit 1;

    if v_created is not null then
        v_wait := greatest(1, ceil(90 - extract(epoch from (now() - v_created)))::int);
        return jsonb_build_object(
            'error',
            'یک سفارش پرداخت باز دارید؛ ابتدا همان را تکمیل یا لغو کنید یا '
                || translate(v_wait::text, '0123456789', '۰۱۲۳۴۵۶۷۸۹')
                || ' ثانیه صبر کنید',
            'wait_seconds', v_wait
        );
    end if;

    insert into public.wallet_payment_orders(user_id, amount_toman, amount_rial, provider)
    values (p_user, p_amount_toman, p_amount_toman * 10, p_provider)
    returning id, operation_id into v_id, v_operation;
    return jsonb_build_object('ok', true, 'id', v_id, 'operation_id', v_operation);
end;
$$;

revoke all on function public.native_create_wallet_payment_order(uuid, bigint, text) from public, anon, authenticated;
grant execute on function public.native_create_wallet_payment_order(uuid, bigint, text) to service_role;

-- ------------------------------------------------------------------ ۲) واریز پس از تأیید درگاه
-- تنها تغییر: سفارشی که با error_code='expired_open_order' آزاد شده هم پذیرفته می‌شود.
-- سفارشِ لغوشده توسط کاربر یا خطای واقعی درگاه همچنان پذیرفته نمی‌شود.
create or replace function public.native_credit_wallet_payment(p_order bigint, p_ref text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_order public.wallet_payment_orders%rowtype;
    v_balance bigint;
begin
    select * into v_order
    from public.wallet_payment_orders
    where id = p_order
    for update;
    if not found then return jsonb_build_object('error', 'سفارش یافت نشد'); end if;
    if v_order.status = 'paid' then
        return jsonb_build_object('ok', true, 'already_paid', true, 'balance', (
            select balance from public.wallets where user_id = v_order.user_id
        ), 'ref_id', v_order.ref_id);
    end if;
    -- V342 — پرداختِ دیرهنگامِ سفارشِ منقضی‌شده هم واریز می‌شود تا پول کاربر گم نشود.
    if v_order.status not in ('requested', 'verifying')
       and not (v_order.status = 'failed' and v_order.error_code = 'expired_open_order') then
        return jsonb_build_object('error', 'سفارش در وضعیت قابل تأیید نیست');
    end if;
    if coalesce(btrim(p_ref), '') = '' or length(p_ref) > 200 then
        return jsonb_build_object('error', 'شماره پیگیری معتبر نیست');
    end if;
    if exists (select 1 from public.wallet_payment_orders where ref_id = p_ref and id <> p_order) then
        return jsonb_build_object('error', 'شماره پیگیری قبلاً مصرف شده است');
    end if;

    insert into public.wallets(user_id, balance) values (v_order.user_id, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_order.user_id for update;
    if v_balance + v_order.amount_toman > 10000000 then
        return jsonb_build_object('error', 'اعتبارسنجی شد اما سقف کیف پول اجازه شارژ نمی‌دهد؛ نیازمند بررسی پشتیبانی');
    end if;

    update public.wallets
    set balance = balance + v_order.amount_toman, updated_at = now()
    where user_id = v_order.user_id
    returning balance into v_balance;

    insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
    values (v_order.user_id, v_order.amount_toman, 'payment:' || v_order.provider, v_balance, v_order.operation_id);

    update public.wallet_payment_orders
    set status = 'paid', ref_id = btrim(p_ref), paid_at = now(), error_code = null
    where id = p_order;

    return jsonb_build_object('ok', true, 'balance', v_balance, 'ref_id', btrim(p_ref));
end;
$$;

revoke all on function public.native_credit_wallet_payment(bigint, text) from public, anon, authenticated;
grant execute on function public.native_credit_wallet_payment(bigint, text) to service_role;

-- بررسی سریع پس از اجرا:
--   select pg_get_functiondef('public.native_create_wallet_payment_order(uuid,bigint,text)'::regprocedure)
--          like '%90 seconds%' as window_90s;
--   select pg_get_functiondef('public.native_credit_wallet_payment(bigint,text)'::regprocedure)
--          like '%expired_open_order%' as late_payment_ok;
