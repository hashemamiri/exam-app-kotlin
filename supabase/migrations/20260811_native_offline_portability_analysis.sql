-- V10: idempotent offline submission, atomic question-centric grading,
-- advanced item analysis, and safe backup/restore.
-- Compatible with safeupdate: every UPDATE/DELETE has a WHERE clause.

begin;

-- ============================================================
-- 1) Idempotent server endpoint for WorkManager submissions
-- ============================================================
create table if not exists public.native_submission_operations (
    operation_id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    exam_id text not null,
    result jsonb not null,
    created_at timestamptz not null default now()
);
create index if not exists idx_native_submission_operations_user
    on public.native_submission_operations(user_id, created_at desc);
alter table public.native_submission_operations enable row level security;

create or replace function public.native_submit_queued_answer_v1(
    p_operation uuid,
    p_exam text,
    p_responses jsonb,
    p_images jsonb,
    p_meta jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    if coalesce(btrim(p_exam), '') = '' then return jsonb_build_object('error', 'شناسه آزمون لازم است'); end if;
    if jsonb_typeof(p_responses) <> 'array' then return jsonb_build_object('error', 'پاسخ‌ها نامعتبرند'); end if;
    if p_images is not null and jsonb_typeof(p_images) <> 'object' then
        return jsonb_build_object('error', 'تصاویر پاسخ نامعتبرند');
    end if;

    perform pg_advisory_xact_lock(hashtextextended(p_operation::text, 104729));
    select result into v_result
    from public.native_submission_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_result || jsonb_build_object('idempotent', true); end if;
    if exists (
        select 1 from public.native_submission_operations
        where operation_id = p_operation and user_id <> v_uid
    ) then return jsonb_build_object('error', 'شناسه عملیات متعلق به حساب دیگری است'); end if;

    if to_regprocedure('public.submit_answer(text,jsonb,jsonb,jsonb)') is not null then
        execute 'select public.submit_answer($1,$2,$3,$4)'
        into v_result
        using p_exam, p_responses, coalesce(p_images, '{}'::jsonb),
              coalesce(p_meta, '{}'::jsonb) || jsonb_build_object('operation_id', p_operation, 'native_queue', true);
    elsif to_regprocedure('public.submit_answer(text,jsonb,jsonb)') is not null then
        execute 'select public.submit_answer($1,$2,$3)'
        into v_result
        using p_exam, p_responses, coalesce(p_images, '{}'::jsonb);
    else
        return jsonb_build_object('error', 'تابع ثبت پاسخ در سرور آماده نیست');
    end if;

    v_result := coalesce(v_result, jsonb_build_object('ok', true));
    if v_result ? 'error' then return v_result; end if;
    insert into public.native_submission_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, p_exam, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb) from public, anon;
grant execute on function public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb) to authenticated;

-- ============================================================
-- 2) Atomic question-centric grading
-- ============================================================
create table if not exists public.native_grade_progress (
    answer_id text not null,
    question_index integer not null check (question_index >= 0),
    teacher_id uuid not null references auth.users(id) on delete cascade,
    updated_at timestamptz not null default now(),
    primary key(answer_id, question_index)
);
create index if not exists idx_native_grade_progress_teacher
    on public.native_grade_progress(teacher_id, updated_at desc);
alter table public.native_grade_progress enable row level security;

create or replace function public.native_bulk_save_question_grades_v1(
    p_exam text,
    p_question_index integer,
    p_items jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_questions jsonb;
    v_question_count integer;
    v_max numeric;
    v_item jsonb;
    v_answer public.answers%rowtype;
    v_grades jsonb;
    v_total numeric;
    v_updated integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select questions into v_questions
    from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    v_question_count := jsonb_array_length(coalesce(v_questions, '[]'::jsonb));
    if p_question_index is null or p_question_index < 0 or p_question_index >= v_question_count then
        return jsonb_build_object('error', 'شماره سؤال نامعتبر است');
    end if;
    v_max := greatest(0, coalesce((v_questions->p_question_index->>'score')::numeric, 0));
    if jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) < 1 or jsonb_array_length(p_items) > 5000 then
        return jsonb_build_object('error', 'فهرست نمره‌ها نامعتبر است');
    end if;
    if (
        select count(*) <> count(distinct item->>'answer_id')
        from jsonb_array_elements(p_items) item
    ) then return jsonb_build_object('error', 'شناسه پاسخ تکراری است'); end if;

    -- Validate the complete batch before changing any row.
    for v_item in select value from jsonb_array_elements(p_items)
    loop
        if coalesce(v_item->>'answer_id', '') = '' then
            return jsonb_build_object('error', 'شناسه یک پاسخ خالی است');
        end if;
        if (v_item->>'score')::numeric < 0 or (v_item->>'score')::numeric > v_max then
            return jsonb_build_object('error', 'یک نمره خارج از بازه صفر تا بارم است');
        end if;
        if not exists (
            select 1 from public.answers a
            where a.id = v_item->>'answer_id' and a.exam_id = p_exam
        ) then return jsonb_build_object('error', 'یک پاسخ متعلق به این آزمون نیست'); end if;
    end loop;

    for v_item in select value from jsonb_array_elements(p_items)
    loop
        select * into v_answer
        from public.answers
        where id = v_item->>'answer_id' and exam_id = p_exam
        for update;

        select jsonb_agg(
            case
                when position = p_question_index then to_jsonb((v_item->>'score')::numeric)
                else coalesce(v_answer.grades->position, '0'::jsonb)
            end order by position
        ) into v_grades
        from generate_series(0, v_question_count - 1) position;

        select coalesce(sum(value::numeric), 0) into v_total
        from jsonb_array_elements_text(v_grades) value;

        update public.answers
        set grades = v_grades,
            total_grade = v_total,
            auto_graded = false
        where id = v_answer.id and exam_id = p_exam;

        insert into public.native_grade_progress(answer_id, question_index, teacher_id, updated_at)
        values (v_answer.id, p_question_index, v_uid, now())
        on conflict (answer_id, question_index) do update
        set teacher_id = excluded.teacher_id, updated_at = excluded.updated_at;
        v_updated := v_updated + 1;
    end loop;

    return jsonb_build_object('ok', true, 'updated', v_updated, 'question_index', p_question_index);
end;
$$;
revoke all on function public.native_bulk_save_question_grades_v1(text,integer,jsonb) from public, anon;
grant execute on function public.native_bulk_save_question_grades_v1(text,integer,jsonb) to authenticated;

create or replace function public.native_finalize_bulk_grades_v1(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_question_count integer;
    v_missing integer;
    v_updated integer;
begin
    select jsonb_array_length(coalesce(questions, '[]'::jsonb)) into v_question_count
    from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;

    select count(*) into v_missing
    from public.answers a
    where a.exam_id = p_exam
      and not coalesce(a.graded, false)
      and (
        select count(distinct p.question_index)
        from public.native_grade_progress p
        where p.answer_id = a.id and p.teacher_id = v_uid
      ) < v_question_count;
    if v_missing > 0 then
        return jsonb_build_object(
            'error', 'برای بعضی پاسخ‌ها هنوز همه سؤال‌ها نمره ندارند',
            'incomplete_answers', v_missing
        );
    end if;

    update public.answers
    set graded = true, auto_graded = false
    where exam_id = p_exam and not coalesce(graded, false);
    get diagnostics v_updated = row_count;
    return jsonb_build_object('ok', true, 'finalized', v_updated);
end;
$$;
revoke all on function public.native_finalize_bulk_grades_v1(text) from public, anon;
grant execute on function public.native_finalize_bulk_grades_v1(text) to authenticated;

-- ============================================================
-- 3) Advanced item analysis
-- ============================================================
create or replace function public.native_question_analysis_v1(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_questions jsonb;
    v_question_count integer;
    v_answer_count integer;
    v_rows jsonb;
    v_alpha double precision;
begin
    select questions into v_questions
    from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    v_question_count := jsonb_array_length(coalesce(v_questions, '[]'::jsonb));
    select count(*) into v_answer_count
    from public.answers
    where exam_id = p_exam and coalesce(graded, false);

    with graded as (
        select a.id, a.total_grade::double precision as total_grade,
               coalesce(a.grades, '[]'::jsonb) as grades,
               coalesce(a.responses, '[]'::jsonb) as responses,
               percent_rank() over (order by a.total_grade) as rank_pct
        from public.answers a
        where a.exam_id = p_exam and coalesce(a.graded, false)
    ), questions as (
        select (ord - 1)::integer as idx,
               q->>'text' as text,
               greatest(0, coalesce((q->>'score')::double precision, 0)) as max_score
        from jsonb_array_elements(coalesce(v_questions, '[]'::jsonb)) with ordinality item(q, ord)
    ), item_values as (
        select q.idx, q.text, q.max_score, g.id, g.total_grade, g.rank_pct,
               case when q.max_score > 0 then coalesce((g.grades->>q.idx)::double precision, 0) / q.max_score else 0 end as ratio,
               g.total_grade - coalesce((g.grades->>q.idx)::double precision, 0) as corrected_total,
               case
                   when g.responses->q.idx is null or g.responses->q.idx = 'null'::jsonb then false
                   when btrim((g.responses->q.idx)::text) in ('""', '{}', '[]') then false
                   else true
               end as answered
        from questions q cross join graded g
    ), aggregates as (
        select idx, max(text) as text, max(max_score) as max_score,
               count(*)::integer as graded_count,
               count(*) filter (where answered)::integer as answered_count,
               coalesce(avg(ratio) * 100, 0) as average_percent,
               coalesce((1 - avg(case when answered then 1.0 else 0.0 end)) * 100, 0) as omit_percent,
               (avg(ratio) filter (where rank_pct >= 0.73) -
                avg(ratio) filter (where rank_pct <= 0.27)) as discrimination,
               corr(ratio, corrected_total) as point_biserial
        from item_values
        group by idx
    )
    select coalesce(jsonb_agg(jsonb_build_object(
        'index', idx,
        'text', text,
        'max_score', max_score,
        'graded_count', graded_count,
        'answered_count', answered_count,
        'average_percent', round(average_percent::numeric, 2),
        'omit_percent', round(omit_percent::numeric, 2),
        'discrimination', case when discrimination is null then null else round(discrimination::numeric, 4) end,
        'point_biserial', case when point_biserial is null then null else round(point_biserial::numeric, 4) end,
        'level', case
            when graded_count >= 5 and coalesce(discrimination, 0) < 0.10 then 'weak_discrimination'
            when average_percent >= 85 then 'very_easy'
            when average_percent >= 70 then 'easy'
            when average_percent <= 30 then 'very_hard'
            when average_percent <= 45 then 'hard'
            else 'balanced'
        end
    ) order by idx), '[]'::jsonb)
    into v_rows
    from aggregates;

    if v_question_count > 1 and v_answer_count > 1 then
        with graded as (
            select a.total_grade::double precision as total_grade,
                   coalesce(a.grades, '[]'::jsonb) as grades
            from public.answers a
            where a.exam_id = p_exam and coalesce(a.graded, false)
        ), item_variances as (
            select idx, var_samp(coalesce((g.grades->>idx)::double precision, 0)) as item_var
            from graded g cross join generate_series(0, v_question_count - 1) idx
            group by idx
        ), totals as (
            select var_samp(total_grade) as total_var from graded
        )
        select case
            when totals.total_var is null or totals.total_var <= 0 then null
            else v_question_count::double precision / (v_question_count - 1) *
                 (1 - coalesce((select sum(item_var) from item_variances), 0) / totals.total_var)
        end into v_alpha
        from totals;
    end if;

    return jsonb_build_object(
        'ok', true,
        'exam_id', p_exam,
        'answer_count', v_answer_count,
        'cronbach_alpha', case when v_alpha is null then null else round(v_alpha::numeric, 4) end,
        'questions', coalesce(v_rows, '[]'::jsonb)
    );
end;
$$;
revoke all on function public.native_question_analysis_v1(text) from public, anon;
grant execute on function public.native_question_analysis_v1(text) to authenticated;

-- ============================================================
-- 4) Safe export and atomic restore
-- ============================================================
create table if not exists public.native_restore_operations (
    operation_id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    result jsonb not null,
    created_at timestamptz not null default now()
);
create index if not exists idx_native_restore_operations_user
    on public.native_restore_operations(user_id, created_at desc);
alter table public.native_restore_operations enable row level security;

create or replace function public.native_export_backup_v1()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_profile jsonb;
    v_classes jsonb;
    v_exams jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم دسترسی دارد');
    end if;

    select jsonb_build_object(
        'full_name', p.full_name,
        'display_name', p.display_name,
        'username', p.username,
        'avatar_url', p.avatar_url,
        'avatar_public', coalesce(p.avatar_public, true),
        'header', jsonb_build_object(
            'province', p.hdr_province,
            'city', p.hdr_city,
            'district', p.hdr_district,
            'school', p.hdr_school
        )
    ) into v_profile
    from public.profiles p
    where p.id = v_uid;

    select coalesce(jsonb_agg(jsonb_build_object(
        'source_id', c.id,
        'name', c.name,
        'grade', c.grade,
        'members', coalesce((
            select jsonb_agg(jsonb_build_object(
                'username', p.username,
                'full_name', p.full_name
            ) order by p.full_name)
            from public.class_members m
            join public.profiles p on p.id = m.student_id
            where m.class_id = c.id and p.teacher_id = v_uid and p.role = 'student'
        ), '[]'::jsonb)
    ) order by c.created_at, c.name), '[]'::jsonb)
    into v_classes
    from public.classes c
    where c.teacher_id = v_uid;

    select coalesce(jsonb_agg(jsonb_build_object(
        'source_id', e.id,
        'title', e.title,
        'subject', e.subject,
        'duration', e.duration,
        'total_score', e.total_score,
        'questions', e.questions,
        'answer_key', coalesce((select k.answers from public.exam_keys k where k.exam_id = e.id), '[]'::jsonb),
        'shuffle_q', e.shuffle_q,
        'shuffle_opt', e.shuffle_opt,
        'neg_marking', e.neg_marking,
        'opens_at', e.opens_at,
        'closes_at', e.closes_at,
        'teacher_message', e.teacher_message,
        'attempts_allowed', e.attempts_allowed,
        'attempt_on_timeout', e.attempt_on_timeout,
        'grade_policy', e.grade_policy,
        'attempt_cooldown', e.attempt_cooldown,
        'audience', coalesce(e.audience, 'all'),
        'audience_classes', coalesce((
            select jsonb_agg(ac.class_id)
            from public.exam_audience_classes ac
            where ac.exam_id = e.id
        ), '[]'::jsonb),
        'audience_students', coalesce((
            select jsonb_agg(p.username)
            from public.exam_audience_students ast
            join public.profiles p on p.id = ast.student_id
            where ast.exam_id = e.id and p.teacher_id = v_uid
        ), '[]'::jsonb)
    ) order by e.created_at), '[]'::jsonb)
    into v_exams
    from public.exams e
    where e.teacher_id = v_uid;

    return jsonb_build_object(
        '_app', 'exam-native',
        '_kind', 'backup',
        '_version', 2,
        'created_at', now(),
        'profile', v_profile,
        'classes', v_classes,
        'exams', v_exams,
        'security', jsonb_build_object(
            'contains_passwords', false,
            'contains_tokens', false,
            'contains_plain_password', false
        )
    );
end;
$$;
revoke all on function public.native_export_backup_v1() from public, anon;
grant execute on function public.native_export_backup_v1() to authenticated;

create or replace function public.native_restore_backup_v1(
    p_operation uuid,
    p_bundle jsonb,
    p_options jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
    v_exams jsonb;
    v_classes jsonb;
    v_profile jsonb;
    v_restore_exams boolean := coalesce((p_options->>'exams')::boolean, true);
    v_restore_classes boolean := coalesce((p_options->>'classes')::boolean, true);
    v_restore_members boolean := coalesce((p_options->>'memberships')::boolean, true);
    v_restore_header boolean := coalesce((p_options->>'header')::boolean, true);
    v_total_questions integer := 0;
    v_cost bigint := 0;
    v_balance bigint := 0;
    v_class_map jsonb := '{}'::jsonb;
    v_class jsonb;
    v_member jsonb;
    v_exam jsonb;
    v_class_id uuid;
    v_student_id uuid;
    v_new_exam_id text;
    v_code text;
    v_try integer;
    v_mode text;
    v_exams_created integer := 0;
    v_classes_created integer := 0;
    v_members_restored integer := 0;
    v_members_missing integer := 0;
    v_inserted integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم دسترسی دارد');
    end if;
    if (p_bundle->>'_app') is distinct from 'exam-native'
       or (p_bundle->>'_kind') is distinct from 'backup' then
        return jsonb_build_object('error', 'فایل پشتیبان معتبر نیست');
    end if;
    if coalesce((p_bundle->>'_version')::integer, 0) not in (1, 2) then
        return jsonb_build_object('error', 'نسخه پشتیبان پشتیبانی نمی‌شود');
    end if;
    v_exams := coalesce(p_bundle->'exams', '[]'::jsonb);
    v_classes := coalesce(p_bundle->'classes', '[]'::jsonb);
    v_profile := coalesce(p_bundle->'profile', '{}'::jsonb);
    if jsonb_typeof(v_exams) <> 'array' or jsonb_array_length(v_exams) > 200 then
        return jsonb_build_object('error', 'فهرست آزمون‌های پشتیبان نامعتبر است');
    end if;
    if jsonb_typeof(v_classes) <> 'array' or jsonb_array_length(v_classes) > 500 then
        return jsonb_build_object('error', 'فهرست کلاس‌های پشتیبان نامعتبر است');
    end if;

    perform pg_advisory_xact_lock(hashtextextended(p_operation::text, 130363));
    select result into v_result
    from public.native_restore_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_result || jsonb_build_object('idempotent', true); end if;
    if exists (
        select 1 from public.native_restore_operations
        where operation_id = p_operation and user_id <> v_uid
    ) then return jsonb_build_object('error', 'شناسه عملیات متعلق به حساب دیگری است'); end if;

    if v_restore_exams then
        select coalesce(sum(jsonb_array_length(coalesce(item->'questions', '[]'::jsonb))), 0)::integer
        into v_total_questions
        from jsonb_array_elements(v_exams) item;
        if v_total_questions > 10000 then
            return jsonb_build_object('error', 'مجموع سؤال‌های پشتیبان بیش از ۱۰٬۰۰۰ است');
        end if;
    end if;
    v_cost := v_total_questions * 1000;
    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول برای بازیابی آزمون‌ها کافی نیست', 'required', v_cost, 'balance', v_balance);
    end if;

    -- Resolve/create classes and build source-id mapping.
    for v_class in select value from jsonb_array_elements(v_classes)
    loop
        if coalesce(v_class->>'source_id', '') = '' then
            raise exception 'شناسه کلاس در پشتیبان خالی است';
        end if;
        if coalesce(btrim(v_class->>'name'), '') = '' or length(v_class->>'name') > 120 then
            raise exception 'نام کلاس در پشتیبان نامعتبر است';
        end if;
        select id into v_class_id
        from public.classes
        where teacher_id = v_uid
          and name = btrim(v_class->>'name')
          and coalesce(grade, '') = coalesce(btrim(v_class->>'grade'), '')
        limit 1;
        if v_class_id is null and v_restore_classes then
            v_class_id := gen_random_uuid();
            insert into public.classes(id, teacher_id, name, grade)
            values (
                v_class_id, v_uid, btrim(v_class->>'name'),
                nullif(btrim(coalesce(v_class->>'grade', '')), '')
            );
            v_classes_created := v_classes_created + 1;
        end if;
        if v_class_id is not null then
            v_class_map := v_class_map || jsonb_build_object(v_class->>'source_id', v_class_id);
        end if;

        if v_restore_members and v_class_id is not null then
            for v_member in select value from jsonb_array_elements(coalesce(v_class->'members', '[]'::jsonb))
            loop
                select id into v_student_id
                from public.profiles
                where teacher_id = v_uid and role = 'student'
                  and username = btrim(v_member->>'username')
                limit 1;
                if v_student_id is null then
                    v_members_missing := v_members_missing + 1;
                else
                    insert into public.class_members(class_id, student_id)
                    values (v_class_id, v_student_id)
                    on conflict do nothing;
                    get diagnostics v_inserted = row_count;
                    v_members_restored := v_members_restored + v_inserted;
                end if;
            end loop;
        end if;
    end loop;

    if v_restore_header and jsonb_typeof(v_profile->'header') = 'object' then
        update public.profiles
        set hdr_province = nullif(btrim(coalesce(v_profile->'header'->>'province', '')), ''),
            hdr_city = nullif(btrim(coalesce(v_profile->'header'->>'city', '')), ''),
            hdr_district = nullif(btrim(coalesce(v_profile->'header'->>'district', '')), ''),
            hdr_school = nullif(btrim(coalesce(v_profile->'header'->>'school', '')), '')
        where id = v_uid;
    end if;

    if v_restore_exams then
        for v_exam in select value from jsonb_array_elements(v_exams)
        loop
            if coalesce(btrim(v_exam->>'title'), '') = '' or length(v_exam->>'title') > 250 then
                raise exception 'عنوان آزمون در پشتیبان نامعتبر است';
            end if;
            if jsonb_typeof(v_exam->'questions') <> 'array'
               or jsonb_array_length(v_exam->'questions') < 1
               or jsonb_array_length(v_exam->'questions') > 500 then
                raise exception 'سؤال‌های یک آزمون نامعتبرند';
            end if;
            if jsonb_typeof(coalesce(v_exam->'answer_key', '[]'::jsonb)) <> 'array' then
                raise exception 'کلید پاسخ یک آزمون نامعتبر است';
            end if;
            v_new_exam_id := gen_random_uuid()::text;
            v_try := 0;
            loop
                v_try := v_try + 1;
                v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
                exit when not exists (select 1 from public.exams where code = v_code);
                if v_try >= 20 then raise exception 'ساخت کد یکتا ناموفق بود'; end if;
            end loop;
            v_mode := case when v_exam->>'audience' in ('all', 'classes', 'students') then v_exam->>'audience' else 'all' end;

            insert into public.exams(
                id, teacher_id, title, subject, duration, code, total_score, is_open,
                shuffle_q, shuffle_opt, neg_marking, opens_at, closes_at, audience,
                teacher_message, attempts_allowed, attempt_on_timeout, grade_policy,
                attempt_cooldown, questions
            ) values (
                v_new_exam_id, v_uid, btrim(v_exam->>'title'), btrim(coalesce(v_exam->>'subject', '')),
                greatest(0, least(1440, coalesce((v_exam->>'duration')::integer, 0))),
                v_code, greatest(0, coalesce((v_exam->>'total_score')::double precision, 0)), false,
                coalesce((v_exam->>'shuffle_q')::boolean, false),
                coalesce((v_exam->>'shuffle_opt')::boolean, false),
                greatest(0, coalesce((v_exam->>'neg_marking')::numeric, 0)),
                nullif(v_exam->>'opens_at', '')::timestamptz,
                nullif(v_exam->>'closes_at', '')::timestamptz,
                v_mode, nullif(btrim(coalesce(v_exam->>'teacher_message', '')), ''),
                greatest(1, least(5, coalesce((v_exam->>'attempts_allowed')::integer, 1))),
                coalesce((v_exam->>'attempt_on_timeout')::boolean, false),
                case when v_exam->>'grade_policy' in ('last','best','all') then v_exam->>'grade_policy' else 'last' end,
                greatest(0, least(1440, coalesce((v_exam->>'attempt_cooldown')::integer, 0))),
                v_exam->'questions'
            );
            insert into public.exam_keys(exam_id, answers)
            values (v_new_exam_id, coalesce(v_exam->'answer_key', '[]'::jsonb));

            if v_mode = 'classes' then
                insert into public.exam_audience_classes(exam_id, class_id)
                select v_new_exam_id, (v_class_map->>value)::uuid
                from jsonb_array_elements_text(coalesce(v_exam->'audience_classes', '[]'::jsonb)) source(value)
                where v_class_map ? value
                on conflict do nothing;
            elsif v_mode = 'students' then
                insert into public.exam_audience_students(exam_id, student_id)
                select v_new_exam_id, p.id
                from jsonb_array_elements_text(coalesce(v_exam->'audience_students', '[]'::jsonb)) source(username)
                join public.profiles p on p.username = source.username
                where p.teacher_id = v_uid and p.role = 'student'
                on conflict do nothing;
            end if;
            v_exams_created := v_exams_created + 1;
        end loop;
    end if;

    if v_cost > 0 then
        update public.wallets
        set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid
        returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'backup:restore:' || p_operation::text, v_balance, p_operation);
    end if;

    v_result := jsonb_build_object(
        'ok', true,
        'exams_created', v_exams_created,
        'classes_created', v_classes_created,
        'memberships_restored', v_members_restored,
        'memberships_missing', v_members_missing,
        'cost', v_cost,
        'balance', v_balance
    );
    insert into public.native_restore_operations(operation_id, user_id, result)
    values (p_operation, v_uid, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_restore_backup_v1(uuid,jsonb,jsonb) from public, anon;
grant execute on function public.native_restore_backup_v1(uuid,jsonb,jsonb) to authenticated;

commit;
notify pgrst, 'reload schema';
