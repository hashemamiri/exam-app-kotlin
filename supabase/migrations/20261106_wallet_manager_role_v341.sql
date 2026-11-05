-- V341 — رفع پس‌رفت: شارژ کیف پول «مدیر» دوباره مجاز شد
--
-- چرا این فایل لازم است:
--   • V38 (20260815_native_manager_wallet_stats_v38.sql) صریحاً پرداخت بانکی را برای مدیر
--     هم مجاز کرده بود: role in ('teacher','manager').
--   • V335.32 (20261026_wallet_zibal_provider.sql و دو فایل دستی sql/manual/SQL_WALLET_*_V335_32.sql)
--     همین تابع را از روی الگوی قدیمیِ «فقط معلم» بازسازی کرد تا درگاه زیبال اضافه شود؛
--     در نتیجه نقش manager ناخواسته حذف شد و مدیر هنگام شارژ پیام
--     «حساب معلم معتبر نیست» می‌گرفت.
--   • این مهاجرت هر دو را با هم دارد: نقش teacher/manager + درگاه zibal + همهٔ محافظ‌های
--     V335.32 (انقضای سفارش نیمه‌کاره، سفارش باز یکتا، سقف موجودی).
--
-- امنیت: مثل قبل security definer با search_path ثابت، و اجرا فقط برای service_role
-- (یعنی تنها Edge Function کیف پول می‌تواند صدایش کند؛ کاربر مستقیم دسترسی ندارد).
-- هیچ داده‌ای تغییر نمی‌کند و تکرار این فایل بی‌خطر است.
--
-- اجرا: Supabase → SQL Editor (یک‌بار).

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

revoke all on function public.native_create_wallet_payment_order(uuid, bigint, text) from public, anon, authenticated;
grant execute on function public.native_create_wallet_payment_order(uuid, bigint, text) to service_role;

-- بررسی سریع پس از اجرا (باید t برگرداند):
--   select pg_get_functiondef('public.native_create_wallet_payment_order(uuid,bigint,text)'::regprocedure)
--          like '%''teacher'', ''manager''%' as manager_allowed;
