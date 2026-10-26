-- V335.32 — افزودن درگاه زیبال به جدول سفارش‌های کیف پول
--
-- چرا: جدول wallet_payment_orders و تابع native_create_wallet_payment_order فقط سه مقدار
-- zarinpal/idpay/sandbox را می‌پذیرفتند؛ با فعال شدن زیبال، ساخت سفارش با خطای
-- «درگاه نامعتبر است» رد می‌شد. این مهاجرت فقط همین قید را باز می‌کند و هیچ داده‌ای را تغییر نمی‌دهد.
--
-- اجرا: Supabase → SQL Editor (یک‌بار). تکرارش هم بی‌خطر است.

-- ۱) قید check ستون provider را با نسخهٔ تازه عوض می‌کنیم (نام قید خودکار است، پس پویا پیدا می‌شود).
do $$
declare c record;
begin
    for c in
        select conname
        from pg_constraint
        where conrelid = 'public.wallet_payment_orders'::regclass
          and contype = 'c'
          and pg_get_constraintdef(oid) ilike '%provider%'
    loop
        execute format('alter table public.wallet_payment_orders drop constraint %I', c.conname);
    end loop;
end $$;

alter table public.wallet_payment_orders
    add constraint wallet_payment_orders_provider_check
    check (provider in ('zarinpal', 'idpay', 'sandbox', 'zibal'));

-- ۲) همان تابع قبلی، فقط با مجاز شدن zibal.
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
begin
    if p_user is null or not exists (
        select 1 from public.profiles where id = p_user and role = 'teacher'
    ) then return jsonb_build_object('error', 'حساب معلم معتبر نیست'); end if;
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
    -- سفارش نیمه‌کاره ناشی از قطع شبکه برای همیشه حساب را قفل نمی‌کند.
    update public.wallet_payment_orders
    set status = 'failed', error_code = 'expired_open_order'
    where user_id = p_user
      and status in ('pending', 'requested', 'verifying')
      and created_at < now() - interval '30 minutes';

    if exists (
        select 1 from public.wallet_payment_orders
        where user_id = p_user and status in ('pending', 'requested', 'verifying')
    ) then return jsonb_build_object('error', 'یک سفارش پرداخت باز دارید؛ ابتدا همان را تکمیل یا لغو کنید'); end if;

    insert into public.wallet_payment_orders(user_id, amount_toman, amount_rial, provider)
    values (p_user, p_amount_toman, p_amount_toman * 10, p_provider)
    returning id, operation_id into v_id, v_operation;
    return jsonb_build_object('ok', true, 'id', v_id, 'operation_id', v_operation);
end;
$$;

revoke all on function public.native_create_wallet_payment_order(uuid,bigint,text) from public, anon, authenticated;
grant execute on function public.native_create_wallet_payment_order(uuid,bigint,text) to service_role;
