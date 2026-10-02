-- V202.1 — هزینهٔ چاپ آزمون‌های آنلاین/محلی (مسیر V132) روی سرور «به‌خاطر سپرده» می‌شود تا
-- پرداخت در اپ، سایت دسکتاپ یا سایت گوشی یکی باشد و دوباره کسر نشود.
-- قانون (همان V199): یک پرداخت = نسخهٔ دانش‌آموز + کلید؛ تا وقتی محتوای آزمون عوض نشده،
-- چاپ دوباره در هر دستگاه/سایتی رایگان است؛ با تغییر محتوا (ذخیرهٔ مجدد) دوباره هزینه دارد.
-- هشِ محتوا: آزمون سروری (uuid در exams) → md5(title|questions) روی سرور؛ پیش‌نویس محلی («local») → md5(p_content کلاینت).

begin;

create table if not exists public.exam_print_payments (
    teacher_id   uuid not null references auth.users(id) on delete cascade,
    exam_ref     text not null,
    content_hash text not null default '',
    questions    integer not null default 0,
    paid_total   bigint not null default 0,
    paid_at      timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    primary key (teacher_id, exam_ref)
);
alter table public.exam_print_payments enable row level security;
drop policy if exists p_exam_print_payments_own on public.exam_print_payments;
create policy p_exam_print_payments_own on public.exam_print_payments for select to authenticated using (teacher_id = auth.uid());
revoke all on public.exam_print_payments from anon;

-- هش و تعداد سؤال برای یک مرجع آزمون؛ خروجی: {hash, questions, server}
create or replace function public.native_print_content_v202(p_exam text, p_questions integer, p_content text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_row record;
    v_id uuid;
begin
    begin
        v_id := p_exam::uuid;
    exception when others then
        v_id := null;
    end;
    if v_id is not null then
        select title, questions into v_row from public.exams where id = v_id and teacher_id = v_uid;
        if found then
            return jsonb_build_object(
                'hash', md5(coalesce(v_row.title, '') || '|' || coalesce(v_row.questions::text, '')),
                'questions', jsonb_array_length(coalesce(v_row.questions, '[]'::jsonb)),
                'server', true);
        end if;
    end if;
    return jsonb_build_object(
        'hash', md5(coalesce(p_content, '') || '|' || greatest(0, least(coalesce(p_questions, 0), 500))::text),
        'questions', greatest(0, least(coalesce(p_questions, 0), 500)),
        'server', false);
end;
$$;
revoke all on function public.native_print_content_v202(text, integer, text) from public, anon, authenticated;

-- برآورد: {paid, due, questions, cost_per_q}
create or replace function public.native_charge_print_quote_v2(p_exam text, p_questions integer default 0, p_content text default '')
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_c jsonb;
    v_paid boolean := false;
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_c := public.native_print_content_v202(p_exam, p_questions, p_content);
    v_count := (v_c->>'questions')::integer;
    select true into v_paid from public.exam_print_payments
    where teacher_id = v_uid and exam_ref = coalesce(p_exam, '') and content_hash = (v_c->>'hash');
    v_paid := coalesce(v_paid, false);
    return jsonb_build_object('paid', v_paid, 'due', case when v_paid then 0 else v_count * 1000 end,
        'questions', v_count, 'cost_per_q', 1000, 'server', v_c->'server');
end;
$$;
revoke all on function public.native_charge_print_quote_v2(text, integer, text) from public, anon;
grant execute on function public.native_charge_print_quote_v2(text, integer, text) to authenticated;

-- کسر: اگر همین محتوا قبلاً پرداخت شده → cost=0 و already_paid=true (بدون تراکنش)
create or replace function public.native_charge_print_v2(
    p_exam text,
    p_operation uuid,
    p_questions integer,
    p_mode text default 'student',
    p_content text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_c jsonb;
    v_hash text;
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

    v_c := public.native_print_content_v202(p_exam, p_questions, p_content);
    v_hash := v_c->>'hash';
    v_count := (v_c->>'questions')::integer;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;

    if exists (select 1 from public.exam_print_payments
               where teacher_id = v_uid and exam_ref = coalesce(p_exam, '') and content_hash = v_hash) then
        return jsonb_build_object('ok', true, 'already_paid', true, 'billed_questions', 0, 'cost', 0,
            'balance', v_balance, 'mode', v_mode);
    end if;

    v_cost := v_count * 1000;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;
    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'exam:print:' || v_mode || ':' || coalesce(p_exam, ''), v_balance, p_operation);
    end if;

    insert into public.exam_print_payments(teacher_id, exam_ref, content_hash, questions, paid_total, paid_at, updated_at)
    values (v_uid, coalesce(p_exam, ''), v_hash, v_count, v_cost, now(), now())
    on conflict (teacher_id, exam_ref) do update set
        content_hash = excluded.content_hash, questions = excluded.questions,
        paid_total = public.exam_print_payments.paid_total + excluded.paid_total,
        paid_at = now(), updated_at = now();

    v_result := jsonb_build_object(
        'ok', true, 'already_paid', false, 'billed_questions', v_count, 'cost', v_cost, 'balance', v_balance, 'mode', v_mode
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, coalesce(p_exam, ''), v_result);
    return v_result;
end;
$$;
revoke all on function public.native_charge_print_v2(text, uuid, integer, text, text) from public, anon;
grant execute on function public.native_charge_print_v2(text, uuid, integer, text, text) to authenticated;

commit;
notify pgrst, 'reload schema';
