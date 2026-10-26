-- ============================================================
--  رفع فوری «یک سفارش پرداخت باز دارید»  (V335.32)
--  در Supabase: منوی چپ -> SQL Editor -> New query
--  کل این فایل را بچسبانید و Run بزنید. هر بار اجرا بی‌خطر است.
-- ============================================================

-- ۱) ببینید چه سفارشی باز مانده است (اختیاری، فقط نمایش)
select id, amount_toman, provider, status, created_at
from public.wallet_payment_orders
where status in ('pending', 'requested', 'verifying')
order by created_at desc;

-- ۲) قفل را همین حالا باز کنید: سفارش‌های آزمایشی زیبال لغو می‌شوند.
--    پرداخت واقعی هیچ کاربر دیگری دست نمی‌خورد (فقط provider = zibal).
update public.wallet_payment_orders
set status = 'canceled', error_code = 'manual_unblock'
where provider = 'zibal' and status in ('pending', 'requested', 'verifying');

-- ۳) این را هم بزنید تا دفعات بعد فقط ۳ دقیقه منتظر بمانید، نه ۳۰ دقیقه.
--    (سفارش‌های قدیمی‌تر از ۳ دقیقه، خودکار failed می‌شوند و دیگر جلوی سفارش تازه را نمی‌گیرند.)

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
      and created_at < now() - interval '3 minutes';  -- V335.32b: پنجرهٔ انتظار کوتاه شد

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
