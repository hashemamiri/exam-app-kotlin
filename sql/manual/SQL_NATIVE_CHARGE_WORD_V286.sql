-- V286 — هزینهٔ خروجی Word: مبلغ ثابت ۵۰٬۰۰۰ تومان برای هر فایل، کاملاً جدا از هزینهٔ چاپ
-- (پرداخت چاپ، Word را شامل نمی‌شود و برعکس). idempotent با p_operation (native_exam_operations).
-- p_exam: شناسهٔ آزمون آنلاین (text) یا آزمون چاپی؛ فقط برای ثبت در تراکنش.

begin;

create or replace function public.native_charge_word_v286(
    p_exam text,
    p_operation uuid,
    p_title text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_cost bigint := 50000;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    select result into v_prior from public.native_exam_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;

    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;
    update public.wallets set balance = balance - v_cost, updated_at = now()
    where user_id = v_uid returning balance into v_balance;
    insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
    values (v_uid, -v_cost, 'exam:word:' || coalesce(p_exam, '') || ':' || left(coalesce(p_title, ''), 80), v_balance, p_operation);

    v_result := jsonb_build_object('ok', true, 'cost', v_cost, 'balance', v_balance);
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, coalesce(p_exam, ''), v_result);
    return v_result;
end;
$$;
revoke all on function public.native_charge_word_v286(text, uuid, text) from public, anon;
grant execute on function public.native_charge_word_v286(text, uuid, text) to authenticated;

commit;
notify pgrst, 'reload schema';
