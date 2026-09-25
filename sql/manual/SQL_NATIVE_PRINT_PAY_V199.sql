-- V199 — پرداخت هزینهٔ چاپ آزمون چاپی (درخواست کاربر):
--   • ساخت/ذخیرهٔ آزمون چاپی رایگان است (هزینهٔ تصویر در native_print_exam_save_v163 حذف شد).
--   • هزینه فقط هنگام چاپ: ۱۰۰۰ تومان هر سؤال + ۱۰۰۰ تومان هر تصویر؛ یک پرداخت مشترک برای نسخهٔ دانش‌آموز و کلید.
--   • اگر سربرگ عوض شود → کل هزینه دوباره؛ اگر فقط یک سؤال عوض شود → فقط همان سؤال (+ تصاویر تازه‌اش)؛
--     اگر فقط تصویری اضافه شود → فقط همان تصویر. چیزی که قبلاً پرداخت شده دوباره کسر نمی‌شود.
--   • اثر انگشت سربرگ (p_header) از کلاینت می‌آید: فیلدهای f_* پیش‌نمایش به‌جز f_course/f_duration (که از خود آزمون می‌آیند و سرور
--     خودش درس/مدت را به هش می‌افزاید)، مرتب و بدون مقدارهای خالی («k=v» خط‌به‌خط)؛
--     سرور فقط md5 آن را نگه می‌دارد. کلید سؤال = md5(سؤال بدون فیلدهای تصویر)؛ تصاویر = URLهای http(s).
-- توابع: native_print_quote_v199 (برآورد)، native_print_pay_v199 (کسر، idempotent)، native_print_pay_status_v199 (وضعیت همهٔ کارت‌ها).
begin;
set local lock_timeout = '8s';

create table if not exists public.print_exam_payments (
    print_exam_id uuid primary key references public.print_exams(id) on delete cascade,
    teacher_id uuid not null references auth.users(id) on delete cascade,
    header_hash text not null default '',
    q_hashes jsonb not null default '{}'::jsonb,   -- {کلید سؤال: [URL تصاویر پرداخت‌شده]}
    paid_total bigint not null default 0,
    paid_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_print_exam_payments_teacher on public.print_exam_payments(teacher_id);
alter table public.print_exam_payments enable row level security;
drop policy if exists p_print_exam_payments_own on public.print_exam_payments;
create policy p_print_exam_payments_own on public.print_exam_payments for select to authenticated using (teacher_id = auth.uid());
revoke all on public.print_exam_payments from anon;

-- تصاویر یک سؤال (رشته یا {uri})؛ فقط http(s)
create or replace function public.native_print_q_images_v199(q jsonb)
returns text[]
language sql
immutable
set search_path = public, pg_temp
as $$
    select coalesce(array_agg(distinct u), '{}') from (
        select case when jsonb_typeof(e) = 'object' then e->>'uri' else trim(both '"' from e::text) end u
        from (
            select jsonb_array_elements(case when jsonb_typeof(q->'images')='array' then q->'images' else '[]'::jsonb end) e
            union all select to_jsonb(q->>'image') where q ? 'image'
            union all select jsonb_array_elements(case when jsonb_typeof(q->'optionImages')='array' then q->'optionImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'leftImages')='array' then q->'leftImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'rightImages')='array' then q->'rightImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'matchingLeftImages')='array' then q->'matchingLeftImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'matchingRightImages')='array' then q->'matchingRightImages' else '[]'::jsonb end)
        ) s where e is not null and jsonb_typeof(e) <> 'null'
    ) t where u is not null and u ~* '^https?://';
$$;
revoke all on function public.native_print_q_images_v199(jsonb) from public, anon, authenticated;

create or replace function public.native_print_q_key_v199(q jsonb)
returns text
language sql
immutable
set search_path = public, pg_temp
as $$
    select md5((q - 'images' - 'image' - 'optionImages' - 'leftImages' - 'rightImages' - 'matchingLeftImages' - 'matchingRightImages' - 'i')::text);
$$;
revoke all on function public.native_print_q_key_v199(jsonb) from public, anon, authenticated;

-- هش سربرگ = اثر انگشت کلاینت (فیلدهای f_* به‌جز f_course/f_duration) + درس و مدت خودِ آزمون چاپی (که در سربرگ چاپ می‌شوند)
create or replace function public.native_print_header_hash_v199(p_exam public.print_exams, p_header text)
returns text
language sql
immutable
set search_path = public, pg_temp
as $$
    select md5(coalesce(p_header, '') || '|' || coalesce(p_exam.subject, '') || '|' || coalesce(p_exam.duration, 0)::text);
$$;
revoke all on function public.native_print_header_hash_v199(public.print_exams, text) from public, anon, authenticated;

-- محاسبهٔ بدهی یک آزمون چاپی برای اثر انگشت سربرگ داده‌شده؛ خروجی: {due, questions_due, images_due, header_changed, current}
create or replace function public.native_print_due_v199(p_exam public.print_exams, p_header_hash text)
returns jsonb
language plpgsql
stable
set search_path = public, pg_temp
as $$
declare
    v_pay public.print_exam_payments%rowtype;
    v_full boolean := true;
    v_paid_imgs text[] := '{}';
    v_q jsonb;
    v_key text;
    v_imgs text[];
    v_img text;
    v_qdue integer := 0;
    v_idue integer := 0;
    v_current jsonb := '{}'::jsonb;
    v_paid_for_key jsonb;
    v_has boolean := false;
begin
    select * into v_pay from public.print_exam_payments where print_exam_id = p_exam.id;
    v_has := found;
    if v_has and v_pay.header_hash = p_header_hash then v_full := false; end if;
    if not v_full then
        select coalesce(array_agg(distinct x), '{}') into v_paid_imgs
        from jsonb_each(v_pay.q_hashes) kv, jsonb_array_elements_text(case when jsonb_typeof(kv.value)='array' then kv.value else '[]'::jsonb end) x;
    end if;
    for v_q in select * from jsonb_array_elements(coalesce(p_exam.questions, '[]'::jsonb)) loop
        v_key := public.native_print_q_key_v199(v_q);
        v_imgs := public.native_print_q_images_v199(v_q);
        v_current := v_current || jsonb_build_object(v_key, to_jsonb(v_imgs));
        if v_full then
            v_qdue := v_qdue + 1;
            v_idue := v_idue + coalesce(array_length(v_imgs, 1), 0);
        else
            v_paid_for_key := v_pay.q_hashes -> v_key;
            if v_paid_for_key is null then v_qdue := v_qdue + 1; end if;
            foreach v_img in array v_imgs loop
                -- تصویر پرداخت‌شده در هر سؤالِ همین آزمون دوباره حساب نمی‌شود
                if not (v_img = any (v_paid_imgs)) then v_idue := v_idue + 1; end if;
            end loop;
        end if;
    end loop;
    return jsonb_build_object(
        'due', (v_qdue + v_idue) * 1000, 'questions_due', v_qdue, 'images_due', v_idue,
        'header_changed', (v_full and v_has), 'never_paid', (not v_has), 'current', v_current
    );
end;
$$;
revoke all on function public.native_print_due_v199(public.print_exams, text) from public, anon, authenticated;

create or replace function public.native_print_quote_v199(p_id uuid, p_header text default '')
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam public.print_exams%rowtype;
    v_due jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select * into v_exam from public.print_exams where id = p_id and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون چاپی پیدا نشد'); end if;
    v_due := public.native_print_due_v199(v_exam, public.native_print_header_hash_v199(v_exam, p_header));
    return (v_due - 'current') || jsonb_build_object('ok', true, 'id', v_exam.id, 'paid', ((v_due->>'due')::bigint = 0),
        'question_count', jsonb_array_length(v_exam.questions), 'per_item', 1000);
end;
$$;
revoke all on function public.native_print_quote_v199(uuid, text) from public, anon;
grant execute on function public.native_print_quote_v199(uuid, text) to authenticated;

-- وضعیت همهٔ آزمون‌های چاپی معلم برای سربرگ فعلی (کارت‌ها: چاپگر قرمز/سبز)
create or replace function public.native_print_pay_status_v199(p_header text default '')
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam public.print_exams%rowtype;
    v_due jsonb;
    v_out jsonb := '[]'::jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    for v_exam in select * from public.print_exams where teacher_id = v_uid loop
        v_due := public.native_print_due_v199(v_exam, public.native_print_header_hash_v199(v_exam, p_header));
        v_out := v_out || jsonb_build_object('id', v_exam.id, 'due', (v_due->>'due')::bigint, 'paid', ((v_due->>'due')::bigint = 0),
            'header_changed', (v_due->>'header_changed')::boolean, 'never_paid', (v_due->>'never_paid')::boolean);
    end loop;
    return v_out;
end;
$$;
revoke all on function public.native_print_pay_status_v199(text) from public, anon;
grant execute on function public.native_print_pay_status_v199(text) to authenticated;

create or replace function public.native_print_pay_v199(p_id uuid, p_operation uuid, p_header text default '')
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam public.print_exams%rowtype;
    v_due jsonb;
    v_cost bigint;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
    v_hash text;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    select result into v_prior from public.native_exam_operations where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;
    select * into v_exam from public.print_exams where id = p_id and teacher_id = v_uid for update;
    if not found then return jsonb_build_object('error', 'آزمون چاپی پیدا نشد'); end if;

    v_hash := public.native_print_header_hash_v199(v_exam, p_header);
    v_due := public.native_print_due_v199(v_exam, v_hash);
    v_cost := (v_due->>'due')::bigint;

    insert into public.wallets(user_id, balance) values (v_uid, 0) on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;
    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now() where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'print_exam:print:' || v_exam.id::text, v_balance, p_operation);
    end if;

    insert into public.print_exam_payments(print_exam_id, teacher_id, header_hash, q_hashes, paid_total, paid_at, updated_at)
    values (v_exam.id, v_uid, v_hash, v_due->'current', v_cost, now(), now())
    on conflict (print_exam_id) do update set
        header_hash = excluded.header_hash,
        -- سربرگ عوض شده → فقط وضعیت فعلی؛ وگرنه پرداخت‌های قبلی + فعلی (تصویر حذف‌شده و برگشته دوباره حساب نمی‌شود)
        q_hashes = case when public.print_exam_payments.header_hash = excluded.header_hash
                        then public.print_exam_payments.q_hashes || excluded.q_hashes else excluded.q_hashes end,
        paid_total = public.print_exam_payments.paid_total + excluded.paid_total,
        paid_at = now(), updated_at = now();

    v_result := jsonb_build_object('ok', true, 'id', v_exam.id, 'cost', v_cost, 'balance', v_balance, 'paid', true,
        'questions_due', v_due->'questions_due', 'images_due', v_due->'images_due');
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result) values (p_operation, v_uid, 'print-pay:' || v_exam.id::text, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_print_pay_v199(uuid, uuid, text) from public, anon;
grant execute on function public.native_print_pay_v199(uuid, uuid, text) to authenticated;

-- ذخیرهٔ آزمون چاپی بدون کسر هزینهٔ تصویر (بدنهٔ V163 با هزینهٔ صفر؛ billed_images فقط برای سازگاری می‌ماند)
create or replace function public.native_print_exam_save_v163(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_id uuid;
    v_operation uuid;
    v_questions jsonb;
    v_old public.print_exams%rowtype;
    v_prior jsonb;
    v_old_billed jsonb := '[]'::jsonb;
    v_new_urls text[];
    v_cost bigint := 0;
    v_new_count integer := 0;
    v_balance bigint;
    v_result jsonb;
    v_all_billed jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم می‌تواند آزمون چاپی ذخیره کند');
    end if;
    begin
        v_id := (p_payload->>'id')::uuid;
        v_operation := (p_payload->>'operation_id')::uuid;
    exception when others then
        return jsonb_build_object('error', 'شناسه نامعتبر است');
    end;
    if v_id is null or v_operation is null then return jsonb_build_object('error', 'شناسه لازم است'); end if;
    v_questions := coalesce(p_payload->'questions', '[]'::jsonb);
    if jsonb_typeof(v_questions) <> 'array' or jsonb_array_length(v_questions) < 1 or jsonb_array_length(v_questions) > 500 then
        return jsonb_build_object('error', 'تعداد سؤال باید بین ۱ و ۵۰۰ باشد');
    end if;
    if pg_column_size(v_questions) > 8 * 1024 * 1024 then
        return jsonb_build_object('error', 'حجم آزمون چاپی بیش از ۸ مگابایت است');
    end if;
    -- تصویر data: نباید ذخیره شود (کلاینت باید قبلاً آپلود کرده باشد)
    if exists (select 1 from jsonb_array_elements(v_questions) q where q::text ~ 'data:image/') then
        return jsonb_build_object('error', 'تصاویر باید قبل از ذخیره آپلود شوند');
    end if;

    select result into v_prior from public.native_exam_operations where operation_id = v_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = v_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    select * into v_old from public.print_exams where id = v_id for update;
    if found then
        if v_old.teacher_id <> v_uid then return jsonb_build_object('error', 'این آزمون چاپی متعلق به شما نیست'); end if;
        v_old_billed := coalesce(v_old.billed_images, '[]'::jsonb);
    end if;
    -- تداخل با آزمون آنلاین: شناسه نباید در exams باشد
    if exists (select 1 from public.exams e where e.id::text = v_id::text) then
        return jsonb_build_object('error', 'این شناسه متعلق به یک آزمون آنلاین است');
    end if;

    select coalesce(array_agg(u), '{}') into v_new_urls
    from public.native_print_image_urls_v163(v_questions) u
    where not (v_old_billed ? u);
    v_new_count := coalesce(array_length(v_new_urls, 1), 0);
    v_cost := 0; -- V199: ذخیرهٔ آزمون چاپی رایگان است؛ هزینه فقط هنگام چاپ (native_print_pay_v199)

    insert into public.wallets(user_id, balance) values (v_uid, 0) on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;
    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now() where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'print_exam:images:' || v_id::text, v_balance, v_operation);
    end if;
    v_all_billed := v_old_billed || coalesce(to_jsonb(v_new_urls), '[]'::jsonb);

    insert into public.print_exams(id, teacher_id, title, subject, duration, questions, source_exam_id, billed_images, updated_at)
    values (v_id, v_uid, left(coalesce(p_payload->>'title',''), 250), left(coalesce(p_payload->>'subject',''), 250),
            greatest(0, least(coalesce((p_payload->>'duration')::integer, 0), 1440)), v_questions,
            nullif(p_payload->>'source_exam_id',''), v_all_billed, now())
    on conflict (id) do update set
        title = excluded.title, subject = excluded.subject, duration = excluded.duration, questions = excluded.questions,
        source_exam_id = coalesce(excluded.source_exam_id, public.print_exams.source_exam_id),
        billed_images = excluded.billed_images, updated_at = now();

    v_result := jsonb_build_object('ok', true, 'id', v_id, 'billed_images', v_new_count, 'cost', v_cost, 'balance', v_balance);
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result) values (v_operation, v_uid, 'print:' || v_id::text, v_result);
    return v_result;
exception when invalid_text_representation then
    return jsonb_build_object('error', 'مدت آزمون نامعتبر است');
end;
$$;
revoke all on function public.native_print_exam_save_v163(jsonb) from public, anon;
grant execute on function public.native_print_exam_save_v163(jsonb) to authenticated;

commit;
notify pgrst, 'reload schema';
