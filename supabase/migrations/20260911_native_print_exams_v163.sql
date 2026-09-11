-- V163 — آزمون‌های چاپی روی سرور (درخواست کاربر): تا معلم از اپ، سایت دسکتاپ و سایت گوشی
-- به همان آزمون‌های چاپی برسد. کاملاً جدا از جدول exams (آزمون آنلاین): جدول مستقل
-- print_exams، بدون کد، بدون مخاطب، بدون exam_keys/answers؛ هیچ تداخلی با آزمون آنلاین ندارد.
-- هزینه: فقط به‌ازای هر تصویر جدید (URL http که در نسخهٔ قبلی همین رکورد نبوده) ۱۰۰۰ تومان؛
-- خودِ ذخیره و سؤال‌ها رایگان. idempotent با p_operation (native_exam_operations).
begin;
set local lock_timeout = '8s';

create table if not exists public.print_exams (
    id uuid primary key,
    teacher_id uuid not null references auth.users(id) on delete cascade,
    title text not null default '',
    subject text not null default '',
    duration integer not null default 0,
    questions jsonb not null default '[]'::jsonb,   -- سؤال‌های کامل (public + key ادغام‌شده، مثل بستهٔ .azmoon)
    source_exam_id text,                            -- آزمون آنلاین مبدأ (برای «نسخهٔ چاپی» بدون تکرار)
    billed_images jsonb not null default '[]'::jsonb, -- URLهایی که قبلاً هزینه‌شان کسر شده
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_print_exams_teacher on public.print_exams(teacher_id, updated_at desc);
alter table public.print_exams enable row level security;
drop policy if exists p_print_exams_own on public.print_exams;
create policy p_print_exams_own on public.print_exams for all to authenticated
    using (teacher_id = auth.uid()) with check (teacher_id = auth.uid());
revoke all on public.print_exams from anon;

-- URLهای تصویری یک آرایهٔ سؤال (تصویر سؤال + گزینه‌ها + جورکردنی)؛ فقط http(s) شمرده می‌شود.
create or replace function public.native_print_image_urls_v163(p_questions jsonb)
returns setof text
language sql
immutable
set search_path = public, pg_temp
as $$
    select distinct u from (
        select jsonb_array_elements_text(case when jsonb_typeof(q->'images')='array' then q->'images' else '[]'::jsonb end) u
        from jsonb_array_elements(coalesce(p_questions,'[]'::jsonb)) q
        union all
        select q->>'image' from jsonb_array_elements(coalesce(p_questions,'[]'::jsonb)) q
        union all
        select jsonb_array_elements_text(case when jsonb_typeof(q->'optionImages')='array' then q->'optionImages' else '[]'::jsonb end)
        from jsonb_array_elements(coalesce(p_questions,'[]'::jsonb)) q
        union all
        select jsonb_array_elements_text(case when jsonb_typeof(q->'leftImages')='array' then q->'leftImages' else '[]'::jsonb end)
        from jsonb_array_elements(coalesce(p_questions,'[]'::jsonb)) q
        union all
        select jsonb_array_elements_text(case when jsonb_typeof(q->'rightImages')='array' then q->'rightImages' else '[]'::jsonb end)
        from jsonb_array_elements(coalesce(p_questions,'[]'::jsonb)) q
    ) s where u is not null and u ~* '^https?://';
$$;
revoke all on function public.native_print_image_urls_v163(jsonb) from public, anon, authenticated;

create or replace function public.native_print_exams_list_v163()
returns jsonb
language sql
security definer
set search_path = public, pg_temp
as $$
    select coalesce(jsonb_agg(jsonb_build_object(
        'id', p.id, 'title', p.title, 'subject', p.subject, 'duration', p.duration,
        'question_count', jsonb_array_length(p.questions), 'source_exam_id', p.source_exam_id,
        'saved_at', (extract(epoch from p.updated_at) * 1000)::bigint
    ) order by p.updated_at desc), '[]'::jsonb)
    from public.print_exams p where p.teacher_id = auth.uid();
$$;
revoke all on function public.native_print_exams_list_v163() from public, anon;
grant execute on function public.native_print_exams_list_v163() to authenticated;

create or replace function public.native_print_exam_get_v163(p_id uuid)
returns jsonb
language sql
security definer
set search_path = public, pg_temp
as $$
    select coalesce((select jsonb_build_object(
        'id', p.id, 'title', p.title, 'subject', p.subject, 'duration', p.duration,
        'questions', p.questions, 'source_exam_id', p.source_exam_id,
        'saved_at', (extract(epoch from p.updated_at) * 1000)::bigint
    ) from public.print_exams p where p.id = p_id and p.teacher_id = auth.uid()),
    jsonb_build_object('error', 'آزمون چاپی پیدا نشد'));
$$;
revoke all on function public.native_print_exam_get_v163(uuid) from public, anon;
grant execute on function public.native_print_exam_get_v163(uuid) to authenticated;

-- ذخیره/به‌روزرسانی. p_payload: {id, operation_id, title, subject, duration, questions, source_exam_id}
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
    v_cost := v_new_count * 1000;

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

create or replace function public.native_print_exam_delete_v163(p_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    delete from public.print_exams where id = p_id and teacher_id = auth.uid();
    if not found then return jsonb_build_object('error', 'آزمون چاپی پیدا نشد'); end if;
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_print_exam_delete_v163(uuid) from public, anon;
grant execute on function public.native_print_exam_delete_v163(uuid) to authenticated;

commit;
notify pgrst, 'reload schema';
