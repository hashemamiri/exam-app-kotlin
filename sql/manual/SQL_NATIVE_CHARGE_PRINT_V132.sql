-- V132 — هزینهٔ چاپ آزمون (۱۰۰۰ تومان به‌ازای هر سؤال) از کیف پول، پیش از بازشدن پنلِ چاپ اندروید.
-- الگو: همان native_duplicate_exam_v2 (idempotent با operation uuid، wallets/wallet_tx، native_exam_operations).
-- تعداد سؤال از کلاینت می‌آید چون آزمون‌های چاپیِ محلی (Room) روی سرور نیستند؛ سقف و کف دارد.
-- ورودی‌ها: p_exam = شناسهٔ آزمون (سروری یا local-…)، p_operation = uuid یکتا، p_questions = تعداد سؤال، p_mode = student|teacher

begin;

create or replace function public.native_charge_print_v1(
    p_exam text,
    p_operation uuid,
    p_questions integer,
    p_mode text default 'student'
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_count integer;
    v_cost bigint;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
    v_mode text := case when p_mode = 'teacher' then 'teacher' else 'student' end;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    select result into v_prior from public.native_exam_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    v_count := greatest(0, least(coalesce(p_questions, 0), 500));
    v_cost := v_count * 1000;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;

    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'exam:print:' || v_mode || ':' || coalesce(p_exam, ''), v_balance, p_operation);
    end if;

    v_result := jsonb_build_object(
        'ok', true, 'billed_questions', v_count, 'cost', v_cost, 'balance', v_balance, 'mode', v_mode
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, coalesce(p_exam, ''), v_result);
    return v_result;
end;
$$;
revoke all on function public.native_charge_print_v1(text,uuid,integer,text) from public, anon;
grant execute on function public.native_charge_print_v1(text,uuid,integer,text) to authenticated;

commit;
notify pgrst, 'reload schema';
