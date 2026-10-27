-- V335.37 / 1.03.18 — preserve per-question penalties, floor only the final total.
-- BACK UP function definitions and test on an isolated database first.
-- Shared Supabase: applying this SQL affects both site branches immediately.
-- No UPDATE/DELETE of historical answers occurs during migration installation.
-- Baseline and uploaded V335.36 function bodies are accepted; no guard bypass.
-- Unknown function definitions stop the whole transaction, with no force option.
begin;
do $guard$
declare r record; v_count integer; v_hash text;
begin
 for r in select * from (values
 ('native_save_grade',ARRAY['a7e71faa3a3283c4ae4adb2804c98e26','5289ac22e719667c510ace1215231c87','6d6d553c6c5ff99d17000c19bdfa6e77','34a6e9d0d62ceaaf0e3e46cf0e2b90c0']),
 ('native_bulk_save_question_grades_v1',ARRAY['cf681fd935bad14c08278c02ee271fb7','b7d28a6c6142234ee7929df1dcc8a1d6','e25eb633ce4a7799f3e3a73b85366073','701584d19ff575e6b9dc699c9391044d']),
 ('native_save_grade_parts_v1',ARRAY['68ce49f8350bb3b9bdb62db3b979e87f','65b02ce263e6f7286cecdfdf53d8cc5c','304ccc65818c66f5ae41b071926dcdb6']),
 ('submit_answer',ARRAY['defb0ff8a9d10a7bd4f3a2767ba2fd3e','73ec519a13fb8c6432faa2dbe145dcef','a5ae3ea6e1785e8396f158cc3b2d09c3']),
 ('approve_auto_grades',ARRAY['98911812124a05bebc65e8f2f0f10941','e6dd545f79a627a73647567dd0ee4e92','65584e200a6b4fc23790f8ce109dc4d1','cdebbfa1230ca6f35e2ec65c5a8ee6c8']),
 ('native_finalize_bulk_grades_v1',ARRAY['9d1b5da0eb633ab95e7d061467a38160','ddad7ceacf54dae965dbf44283af6817','05e46b6ef7fabf33867954e83feb10dc','3df87c853cc1db3af0442b97206f3749']),
 ('exam_autograde_info',ARRAY['c6b208e44abcce00f949c2bd5feb4630','ece718f58c668a6003ad46ad12f4a9c6','ece718f58c668a6003ad46ad12f4a9c6'])
 ) as expected(name,allowed) loop
   select count(*),min(md5(replace(p.prosrc, E'\r\n', E'\n'))) into v_count,v_hash from pg_proc p
     join pg_namespace n on n.oid=p.pronamespace where n.nspname='public' and p.proname=r.name and p.prokind='f';
   if v_count<>1 or not (v_hash=any(r.allowed)) then
     raise exception 'V335.37 safety stop: function % has an unreviewed/missing/overloaded definition; no migration applied. Compare server metadata first.',r.name;
   end if;
 end loop;
end $guard$;
-- V335.37: shared grading policy. No historical answer/score rewrites.
create or replace function public.native_grade_min_v335_37(p_type text, p_score numeric, p_neg numeric)
returns numeric language sql immutable set search_path = public, pg_temp as $$
 select case when lower(coalesce(p_type,'')) in ('multiple','truefalse')
   and coalesce(p_neg,0) > 0 and p_neg::text not in ('NaN','Infinity','-Infinity')
   then -p_neg * greatest(coalesce(p_score,0),0) else 0 end;
$$;

create or replace function public.native_question_shown_v335_37(p_meta jsonb, p_index integer)
returns boolean language sql immutable set search_path = public, pg_temp as $$
 select case when jsonb_typeof(p_meta->'shown_q') = 'array' then
   case when jsonb_array_length(p_meta->'shown_q') > 0
        then (p_meta->'shown_q') @> to_jsonb(p_index) else true end
   else true end;
$$;

create or replace function public.native_auto_grade_with_penalty_v335_37(
 p_type text, p_score numeric, p_key jsonb, p_resp jsonb, p_neg numeric
) returns numeric language plpgsql immutable set search_path = public, pg_temp as $$
declare v_g numeric;
begin
 v_g := public.native_auto_grade_question_v1(p_type,p_score,p_key,p_resp);
 if v_g = 0 and coalesce(p_neg,0) > 0
    and lower(coalesce(p_type,'')) in ('multiple','truefalse')
    and p_resp is not null and jsonb_typeof(p_resp) <> 'null'
    and btrim(p_resp #>> '{}') <> '' then
   return public.native_grade_min_v335_37(p_type,p_score,p_neg);
 end if;
 return v_g;
end;
$$;

create or replace function public.native_save_grade(
    p_answer text,
    p_grades jsonb,
    p_feedback text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam_id text;
    v_questions jsonb;
    v_total numeric := 0;
    v_neg numeric;
    v_min numeric;
    v_value numeric;
    v_max numeric;
    v_index integer;
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if jsonb_typeof(p_grades) is distinct from 'array' then return jsonb_build_object('error', 'ساختار نمره نامعتبر است'); end if;

    select a.exam_id, e.questions, e.neg_marking
    into v_exam_id, v_questions, v_neg
    from public.answers a
    join public.exams e on e.id = a.exam_id
    where a.id = p_answer
      and e.teacher_id = v_uid
    limit 1 for update of a;

    if v_exam_id is null then return jsonb_build_object('error', 'پاسخ یافت نشد یا دسترسی ندارید'); end if;
    if jsonb_array_length(p_grades) <> jsonb_array_length(coalesce(v_questions, '[]'::jsonb)) then
        return jsonb_build_object('error', 'تعداد نمره‌ها با سؤال‌ها برابر نیست');
    end if;

    for v_index in 0..jsonb_array_length(p_grades) - 1 loop
        if jsonb_typeof(p_grades -> v_index) is distinct from 'number' then
            return jsonb_build_object('error','مقدار نمره باید عدد معتبر و غیرخالی باشد');
        end if;
        begin
            v_value := (p_grades ->> v_index)::numeric;
            v_max := coalesce((v_questions -> v_index ->> 'score')::numeric, 0);
        exception when others then
            return jsonb_build_object('error', 'مقدار نمره نامعتبر است');
        end;
        v_min := public.native_grade_min_v335_37(v_questions->v_index->>'type',v_max,v_neg);
        if not public.native_question_shown_v335_37((select meta from public.answers where id=p_answer),v_index)
           and v_value <> 0 then
          return jsonb_build_object('error','سؤال نمایش‌داده‌نشده باید نمره صفر داشته باشد');
        end if;
        if v_value is null or v_value::text in ('NaN','Infinity','-Infinity')
           or v_value < v_min - 0.00000001 or v_value > v_max then
            return jsonb_build_object('error', format('نمره سؤال %s خارج از بازه است', v_index + 1));
        end if;
        v_total := v_total + v_value;
    end loop;

    update public.answers
    set grades = p_grades,
        total_grade = greatest(v_total,0),
        feedback = left(coalesce(p_feedback, ''), 2000),
        graded = true,
        auto_graded = false, -- V260.7: نمرهٔ دستی معلم با تأیید خودکار بعدی بازنویسی نمی‌شود
        graded_at = now()::text
    where id = p_answer
      and exam_id = v_exam_id;
    get diagnostics v_count = row_count;

    if v_count <> 1 then return jsonb_build_object('error', 'ثبت نمره انجام نشد'); end if;
    return jsonb_build_object('ok', true, 'total', greatest(v_total,0));
end;
$$;

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
    v_min numeric;
    v_neg numeric;
    v_item jsonb;
    v_answer public.answers%rowtype;
    v_grades jsonb;
    v_total numeric;
    v_updated integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select questions, neg_marking into v_questions, v_neg
    from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    v_question_count := jsonb_array_length(coalesce(v_questions, '[]'::jsonb));
    if p_question_index is null or p_question_index < 0 or p_question_index >= v_question_count then
        return jsonb_build_object('error', 'شماره سؤال نامعتبر است');
    end if;
    v_max := greatest(0, coalesce((v_questions->p_question_index->>'score')::numeric, 0));
    v_min := public.native_grade_min_v335_37(v_questions->p_question_index->>'type',v_max,v_neg);
    if jsonb_typeof(v_questions->p_question_index->'assessmentParts') = 'array'
       and jsonb_array_length(v_questions->p_question_index->'assessmentParts') > 0 then
       return jsonb_build_object('error','سؤال چندبخشی را در تصحیح فردی ذخیره کنید');
    end if;
    if jsonb_typeof(p_items) is distinct from 'array' or jsonb_array_length(p_items) < 1 or jsonb_array_length(p_items) > 5000 then
        return jsonb_build_object('error', 'فهرست نمره‌ها نامعتبر است');
    end if;
    if (
        select count(*) <> count(distinct item->>'answer_id')
        from jsonb_array_elements(p_items) item
    ) then return jsonb_build_object('error', 'شناسه پاسخ تکراری است'); end if;

    -- Validate the complete batch before changing any row.
    for v_item in select value from jsonb_array_elements(p_items) order by value->>'answer_id'
    loop
        if coalesce(v_item->>'answer_id', '') = '' then
            return jsonb_build_object('error', 'شناسه یک پاسخ خالی است');
        end if;
        if jsonb_typeof(v_item->'score') is distinct from 'number' then
            return jsonb_build_object('error','مقدار نمره باید عدد معتبر و غیرخالی باشد');
        end if;
        if (v_item->>'score')::numeric < v_min - 0.00000001 or (v_item->>'score')::numeric > v_max then
            return jsonb_build_object('error', 'یک نمره خارج از بازه مجاز سؤال است');
        end if;
        select * into v_answer from public.answers a
        where a.id = v_item->>'answer_id' and a.exam_id = p_exam for update;
        if not found then return jsonb_build_object('error', 'یک پاسخ متعلق به این آزمون نیست'); end if;
        if not public.native_question_shown_v335_37(v_answer.meta,p_question_index)
           and (v_item->>'score')::numeric <> 0 then
          return jsonb_build_object('error','سؤال نمایش‌داده‌نشده باید نمره صفر داشته باشد');
        end if;
    end loop;

    for v_item in select value from jsonb_array_elements(p_items) order by value->>'answer_id'
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
            total_grade = greatest(v_total,0),
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

create or replace function public.native_save_grade_parts_v1(
    p_answer text,
    p_grades jsonb,
    p_feedback text default '',
    p_part_grades jsonb default '{}'::jsonb,
    p_part_feedback jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam_id text;
    v_questions jsonb;
    v_meta jsonb;
    v_part_key text;
    v_pair text[];
    v_q integer;
    v_p integer;
    v_value numeric;
    v_max numeric;
    v_question_sum numeric;
    v_index integer;
    v_part_count integer;
    v_result jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if jsonb_typeof(coalesce(p_part_grades, '{}'::jsonb)) <> 'object'
       or jsonb_typeof(coalesce(p_part_feedback, '{}'::jsonb)) <> 'object' then
        return jsonb_build_object('error', 'ساختار نمره یا بازخورد زیربخش نامعتبر است');
    end if;

    select a.exam_id, e.questions, a.meta into v_exam_id, v_questions, v_meta
    from public.answers a join public.exams e on e.id = a.exam_id
    where a.id = p_answer and e.teacher_id = v_uid limit 1 for update of a;
    if v_exam_id is null then return jsonb_build_object('error', 'پاسخ یافت نشد یا دسترسی ندارید'); end if;

    for v_part_key in select jsonb_object_keys(coalesce(p_part_grades, '{}'::jsonb)) loop
        if v_part_key !~ '^[0-9]+:[0-9]+$' then
            return jsonb_build_object('error', 'شناسهٔ زیربخش نامعتبر است');
        end if;
        v_pair := string_to_array(v_part_key, ':');
        v_q := v_pair[1]::integer;
        v_p := v_pair[2]::integer;
        if v_q < 0 or v_q >= jsonb_array_length(coalesce(v_questions, '[]'::jsonb)) then
            return jsonb_build_object('error', 'شمارهٔ سؤال زیربخش نامعتبر است');
        end if;
        if jsonb_typeof(v_questions -> v_q -> 'assessmentParts') <> 'array'
           or v_p < 0 or v_p >= jsonb_array_length(v_questions -> v_q -> 'assessmentParts') then
            return jsonb_build_object('error', 'زیربخش در آزمون پیدا نشد');
        end if;
        if jsonb_typeof(p_part_grades -> v_part_key) is distinct from 'number' then
          return jsonb_build_object('error','نمره زیربخش باید عدد معتبر و غیرخالی باشد');
        end if;
        begin
            v_value := (p_part_grades ->> v_part_key)::numeric;
            v_max := coalesce((v_questions -> v_q -> 'assessmentParts' -> v_p ->> 'score')::numeric, 0);
        exception when others then
            return jsonb_build_object('error', 'نمرهٔ زیربخش نامعتبر است');
        end;
        if v_value is null or v_value::text in ('NaN','Infinity','-Infinity') or v_value < 0 or v_value > v_max then
            return jsonb_build_object('error', format('نمرهٔ زیربخش %s خارج از بازه است', v_p + 1));
        end if;
    end loop;

    for v_index in 0..jsonb_array_length(coalesce(v_questions, '[]'::jsonb)) - 1 loop
        v_part_count := case when jsonb_typeof(v_questions -> v_index -> 'assessmentParts') = 'array'
            then jsonb_array_length(v_questions -> v_index -> 'assessmentParts') else 0 end;
        if v_part_count > 0 and public.native_question_shown_v335_37(v_meta,v_index) then
            select coalesce(sum((p_part_grades ->> (v_index::text || ':' || g::text))::numeric), 0)
              into v_question_sum from generate_series(0, v_part_count - 1) g;
            if abs(v_question_sum - coalesce((p_grades ->> v_index)::numeric, 0)) > 0.000001 then
                return jsonb_build_object('error', format('جمع نمرهٔ زیربخش‌های سؤال %s با نمرهٔ کل برابر نیست', v_index + 1));
            end if;
        end if;
    end loop;

    for v_part_key in select jsonb_object_keys(coalesce(p_part_feedback, '{}'::jsonb)) loop
        if v_part_key !~ '^[0-9]+:[0-9]+$' then
            return jsonb_build_object('error', 'شناسهٔ بازخورد زیربخش نامعتبر است');
        end if;
        v_pair := string_to_array(v_part_key, ':');
        v_q := v_pair[1]::integer;
        v_p := v_pair[2]::integer;
        if v_q < 0 or v_q >= jsonb_array_length(coalesce(v_questions, '[]'::jsonb))
           or jsonb_typeof(v_questions -> v_q -> 'assessmentParts') <> 'array'
           or v_p < 0 or v_p >= jsonb_array_length(v_questions -> v_q -> 'assessmentParts')
           or jsonb_typeof(p_part_feedback -> v_part_key) <> 'string'
           or length(coalesce(p_part_feedback ->> v_part_key, '')) > 1000 then
            return jsonb_build_object('error', 'بازخورد زیربخش نامعتبر یا بیش از حد طولانی است');
        end if;
    end loop;

    v_result := public.native_save_grade(p_answer, p_grades, p_feedback);
    if v_result ? 'error' then return v_result; end if;
    update public.answers
       set part_grades = coalesce(p_part_grades, '{}'::jsonb),
           part_feedback = coalesce(p_part_feedback, '{}'::jsonb)
     where id = p_answer and exam_id = v_exam_id;
    return v_result || jsonb_build_object('part_grades_saved', true);
end;
$$;

create or replace function public.submit_answer(
  p_exam_id text,
  p_responses jsonb,
  p_images jsonb default '{}'::jsonb,
  p_meta jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_exam exams%rowtype;
  v_keys jsonb := '[]'::jsonb;
  v_key_item jsonb;
  v_name text;
  v_grades jsonb := '[]'::jsonb;
  v_total numeric := 0;
  v_q jsonb;
  v_i int;
  v_score numeric;
  v_resp_el jsonb;
  v_auto numeric;
  v_g numeric;
  v_id text;
  v_att exam_attempts%rowtype;
  v_st record;
  v_no int;
  v_shown jsonb := null;
  v_full jsonb;
  v_enforced jsonb := '[]'::jsonb;
  v_sec_deadlines jsonb := '{}'::jsonb;
  v_sec_locked jsonb := '{}'::jsonb;
  v_sec_title text;
  v_sec_dl timestamptz;
  v_queued_ms numeric;
  v_pos int;
  v_n int;
  v_max numeric := 0;
  v_meta jsonb;
begin
  if jsonb_typeof(p_responses) is distinct from 'array' or jsonb_typeof(coalesce(p_meta,'{}')) <> 'object' then
    return jsonb_build_object('error','ساختار پاسخ نامعتبر است');
  end if;
  select * into v_exam from exams where id = p_exam_id;
  if not found then return jsonb_build_object('error','آزمون یافت نشد'); end if;
  if not coalesce(v_exam.is_open,true) then
    return jsonb_build_object('error','آزمون بسته است'); end if;

  select full_name into v_name from profiles where id = auth.uid();
  if v_name is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;

  select * into v_att from exam_attempts
   where exam_id = p_exam_id and student_id = auth.uid()
     and submitted_at is null and abandoned = false
   order by attempt_no desc limit 1;

  if v_att.id is null then
    select * into v_st from attempt_state(p_exam_id, auth.uid());
    if not v_st.can_start then
      return jsonb_build_object('error', v_st.reason);
    end if;
    select coalesce(max(attempt_no), 0) + 1 into v_no
      from exam_attempts where exam_id = p_exam_id and student_id = auth.uid();
    insert into exam_attempts
      (exam_id, student_id, student_name, attempt_no, started_at, expires_at)
    values (p_exam_id, auth.uid(), v_name, v_no, now(),
            case when coalesce(v_exam.duration, 0) > 0
                 then now() + make_interval(mins => v_exam.duration) end)
    on conflict (exam_id, student_id, attempt_no) do update
      set started_at = now(), abandoned = false
    returning * into v_att;
  end if;

  if v_att.expires_at is not null
     and now() > v_att.expires_at + interval '30 seconds' then
    update exam_attempts set abandoned = true where id = v_att.id;
    return jsonb_build_object('error','مهلت آزمون تمام شده است');
  end if;

  v_no := v_att.attempt_no;
  v_sec_deadlines := case when jsonb_typeof(v_att.section_deadlines) = 'object' then v_att.section_deadlines else '{}'::jsonb end;
  v_sec_locked := case when jsonb_typeof(v_att.section_locked_responses) = 'object' then v_att.section_locked_responses else '{}'::jsonb end;

  v_n := coalesce(jsonb_array_length(coalesce(v_exam.questions,'[]'::jsonb)), 0);
  v_shown := v_att.shown_q;
  if v_shown is not null and jsonb_typeof(v_shown) = 'array' and jsonb_array_length(v_shown) > 0
     and jsonb_typeof(p_responses) = 'array' and jsonb_array_length(p_responses) <> v_n then
    v_full := '[]'::jsonb;
    v_pos := 0;
    for v_i in 0 .. greatest(v_n, 1) - 1 loop
      if v_shown @> to_jsonb(v_i) then
        v_full := v_full || jsonb_build_array(coalesce(p_responses -> v_pos, '""'::jsonb));
        v_pos := v_pos + 1;
      else
        v_full := v_full || '[""]'::jsonb;
      end if;
    end loop;
  else
    v_full := coalesce(p_responses, '[]'::jsonb);
    if v_shown is not null and jsonb_typeof(v_shown) <> 'array' then v_shown := null; end if;
    if v_shown is not null and jsonb_array_length(v_shown) = 0 then v_shown := null; end if;
  end if;

  begin
    v_queued_ms := (p_meta ->> 'created_at_epoch_ms')::numeric;
  exception when others then
    v_queued_ms := null;
  end;

  -- اعمال قفل سرور روی پاسخ‌های بخش‌های منقضی‌شده (جلوگیری از دست‌کاری پس از پایان مهلت بخش)
  for v_i in 0 .. greatest(v_n, 0) - 1 loop
    v_q := coalesce(v_exam.questions -> v_i, '{}'::jsonb);
    v_resp_el := coalesce(v_full -> v_i, '""'::jsonb);
    v_sec_title := nullif(btrim(coalesce(v_q ->> 'sectionTitle', '')), '');
    if v_sec_title is not null then
      if v_sec_locked ? v_sec_title then
        v_resp_el := coalesce(v_sec_locked -> v_sec_title -> (v_i::text), '""'::jsonb);
      elsif v_sec_deadlines ? v_sec_title then
        begin
          v_sec_dl := (v_sec_deadlines ->> v_sec_title)::timestamptz;
          if now() > v_sec_dl + interval '30 seconds'
             and not (coalesce((p_meta ->> 'queued')::boolean, false)
                      and v_queued_ms is not null
                      and (v_queued_ms / 1000.0) <= extract(epoch from v_sec_dl + interval '30 seconds')) then
            v_resp_el := '""'::jsonb;
          end if;
        exception when others then null;
        end;
      end if;
    end if;
    v_enforced := v_enforced || jsonb_build_array(v_resp_el);
  end loop;
  v_full := v_enforced;

  select coalesce(answers, '[]'::jsonb) into v_keys from exam_keys where exam_id = p_exam_id;
  v_keys := coalesce(v_keys, '[]'::jsonb);

  v_i := 0;
  for v_q in select * from jsonb_array_elements(coalesce(v_exam.questions,'[]'::jsonb))
  loop
    v_score := coalesce((v_q->>'score')::numeric, 0);
    v_resp_el := coalesce(v_full -> v_i, '""'::jsonb);
    v_g := 0;

    if v_shown is null or v_shown @> to_jsonb(v_i) then
      v_max := v_max + v_score;
      v_key_item := '{}'::jsonb;
      if jsonb_typeof(v_keys) = 'array' then
        select coalesce(item, '{}'::jsonb) into v_key_item
        from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
        where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
        order by ord
        limit 1;
        v_key_item := coalesce(v_key_item, '{}'::jsonb);
      end if;

      v_auto := public.native_auto_grade_with_penalty_v335_37(v_q ->> 'type', v_score, v_key_item, v_resp_el, v_exam.neg_marking);
      if v_auto is not null then
        v_g := v_auto;

      end if;
    end if;

    v_grades := v_grades || to_jsonb(v_g);
    v_total := v_total + v_g;
    v_i := v_i + 1;
  end loop;

  -- Selection metadata belongs to the server, not to the client.
  v_meta := coalesce(p_meta,'{}'::jsonb) - 'shown_q' - 'max_score' - 'pick_count';
  if v_shown is not null then
    v_meta := v_meta || jsonb_build_object('shown_q', v_shown, 'max_score', v_max, 'pick_count', coalesce(v_exam.pick_count,0));
  end if;

  v_id := extract(epoch from now())::bigint::text || '-' || substr(md5(random()::text),1,6);

  insert into answers (id, exam_id, student_id, student_name, responses,
                       response_images, grades, total_grade, graded,
                       submitted_at, meta, attempt_no)
  values (v_id, p_exam_id, auth.uid(), v_name, v_full,
          coalesce(p_images,'{}'::jsonb), v_grades, greatest(v_total,0), false,
          now(), v_meta, v_no);

  update exam_attempts set submitted_at = now() where id = v_att.id;

  delete from exam_sessions
   where exam_id = p_exam_id and student_id = auth.uid();

  select * into v_st from attempt_state(p_exam_id, auth.uid());

  return jsonb_build_object('ok', true,
           'attempt', v_no,
           'attempts_used', v_st.used,
           'attempts_allowed', v_st.max_allowed,
           'can_retry', v_st.can_start,
           'receipt', upper(right(replace(v_id,'-',''), 6)));
exception when unique_violation then
  return jsonb_build_object('error','این تلاش قبلاً ثبت شده است');
end $function$;

create or replace function public.approve_auto_grades(p_exam text,p_mode text default 'auto_only')
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb;
    v_n integer;
    v_i integer;
    v_q jsonb;
    v_key jsonb;
    v_ans record;
    v_score numeric;
    v_resp jsonb;
    v_g numeric;
    v_grades jsonb;
    v_total numeric;
    v_progress boolean;
    v_incomplete boolean;
    v_manual_preserved boolean;
    v_missing_q integer[] := '{}';
    v_objective integer[] := '{}';
    v_missing_answers integer := 0;
    v_count integer := 0;
    v_prefilled integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    select * into v_exam from public.exams e where e.id=p_exam and e.teacher_id=v_uid;
    if not found then return jsonb_build_object('error','آزمون یافت نشد یا دسترسی ندارید'); end if;
    if jsonb_typeof(v_exam.questions) is distinct from 'array' then
      return jsonb_build_object('error','ساختار سؤال‌ها نامعتبر است');
    end if;
    v_n := jsonb_array_length(v_exam.questions);
    select answers into v_keys from public.exam_keys where exam_id=p_exam;
    if jsonb_typeof(v_keys) is distinct from 'array' then v_keys:='[]'; end if;

    if coalesce(p_mode,'auto_only') not in ('auto_only','all') then
      return jsonb_build_object('error','حالت تصحیح نامعتبر است');
    end if;
    -- Preserve finalized manual answers; protect partial manual per-question grades too.
    for v_ans in select a.* from public.answers a where a.exam_id=p_exam
      and (not coalesce(a.graded,false) or coalesce(a.auto_graded,false)) order by a.id for update
    loop
        v_grades := '[]'::jsonb;
        v_total := 0;
        v_objective := '{}';
        v_incomplete := false;
        v_manual_preserved := false;
        for v_i in 0 .. v_n - 1 loop
            v_q := coalesce(v_exam.questions->v_i,'{}'::jsonb);
            v_score := coalesce((v_q->>'score')::numeric,0);
            v_key := '{}'::jsonb;
            select item into v_key from jsonb_array_elements(v_keys) with ordinality as t(item,ord)
            where coalesce((item->>'i')::integer,(ord-1)::integer)=v_i order by ord limit 1;
            v_key := coalesce(v_key,'{}'::jsonb);
            v_progress := exists(select 1 from public.native_grade_progress p where p.answer_id=v_ans.id and p.question_index=v_i);
            if not public.native_question_shown_v335_37(v_ans.meta,v_i) then
                v_g := 0;
            elsif v_progress and not coalesce(v_ans.auto_graded,false) then
                v_manual_preserved := true;
                begin v_g := (v_ans.grades->>v_i)::numeric; exception when others then v_g := null; end;
                if v_g is null then
                    return jsonb_build_object('error','یک نمره ذخیره‌شده نامعتبر است؛ ابتدا تصحیح فردی را بررسی کنید');
                end if;
            else
                v_resp := case when jsonb_typeof(v_ans.responses)='array' then v_ans.responses->v_i else null end;
                v_g := public.native_auto_grade_with_penalty_v335_37(v_q->>'type',v_score,v_key,v_resp,v_exam.neg_marking);
                if v_g is null then
                    v_incomplete := true;
                    if not (v_i+1 = any(v_missing_q)) then v_missing_q := v_missing_q || (v_i+1); end if;
                    begin v_g := coalesce((v_ans.grades->>v_i)::numeric,0); exception when others then v_g:=0; end;
                else
                    v_objective := v_objective || v_i;
                end if;
            end if;
            if v_g is null or v_g::text in ('NaN','Infinity','-Infinity')
               or v_g < public.native_grade_min_v335_37(v_q->>'type',v_score,v_exam.neg_marking)-0.00000001
               or v_g > v_score then
                return jsonb_build_object('error',format('نمره سؤال %s خارج از بازه مجاز است',v_i+1));
            end if;
            v_grades := v_grades || to_jsonb(v_g);
            v_total := v_total + v_g;
        end loop;
        update public.answers set grades=v_grades,total_grade=greatest(v_total,0),
          graded=not v_incomplete, auto_graded=not v_manual_preserved,
          graded_at=case when v_incomplete then null else now()::text end
        where id=v_ans.id and exam_id=p_exam;
        insert into public.native_grade_progress(answer_id,question_index,teacher_id,updated_at)
        select v_ans.id,i,v_uid,now() from unnest(v_objective) as i
        on conflict(answer_id,question_index) do update set updated_at=excluded.updated_at;
        if v_incomplete then v_prefilled:=v_prefilled+1; else v_count:=v_count+1; end if;
    end loop;
    return jsonb_build_object('ok',true,'count',v_count,'prefilled',v_prefilled,
      'has_essay',cardinality(v_missing_q)>0,'essay_questions',to_jsonb(v_missing_q));
end;
$$;

create or replace function public.native_finalize_bulk_grades_v1(p_exam text)
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb;
    v_n integer;
    v_i integer;
    v_q jsonb;
    v_key jsonb;
    v_ans record;
    v_score numeric;
    v_resp jsonb;
    v_g numeric;
    v_grades jsonb;
    v_total numeric;
    v_progress boolean;
    v_incomplete boolean;
    v_manual_preserved boolean;
    v_missing_q integer[] := '{}';
    v_objective integer[] := '{}';
    v_missing_answers integer := 0;
    v_count integer := 0;
    v_prefilled integer := 0;
    v_pending jsonb := '[]'::jsonb;
    v_item jsonb;
begin
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    select * into v_exam from public.exams e where e.id=p_exam and e.teacher_id=v_uid;
    if not found then return jsonb_build_object('error','آزمون یافت نشد یا دسترسی ندارید'); end if;
    if jsonb_typeof(v_exam.questions) is distinct from 'array' then
      return jsonb_build_object('error','ساختار سؤال‌ها نامعتبر است');
    end if;
    v_n := jsonb_array_length(v_exam.questions);
    select answers into v_keys from public.exam_keys where exam_id=p_exam;
    if jsonb_typeof(v_keys) is distinct from 'array' then v_keys:='[]'; end if;

    -- Lock a stable set. Build/validate all results before making any data change.
    for v_ans in select a.* from public.answers a where a.exam_id=p_exam
      and not coalesce(a.graded,false) order by a.id for update
    loop
        v_grades := '[]'::jsonb;
        v_total := 0;
        v_objective := '{}';
        v_incomplete := false;
        v_manual_preserved := false;
        for v_i in 0 .. v_n - 1 loop
            v_q := coalesce(v_exam.questions->v_i,'{}'::jsonb);
            v_score := coalesce((v_q->>'score')::numeric,0);
            v_key := '{}'::jsonb;
            select item into v_key from jsonb_array_elements(v_keys) with ordinality as t(item,ord)
            where coalesce((item->>'i')::integer,(ord-1)::integer)=v_i order by ord limit 1;
            v_key := coalesce(v_key,'{}'::jsonb);
            v_progress := exists(select 1 from public.native_grade_progress p where p.answer_id=v_ans.id and p.question_index=v_i);
            if not public.native_question_shown_v335_37(v_ans.meta,v_i) then
                v_g := 0;
            elsif v_progress and true then
                v_manual_preserved := true;
                begin v_g := (v_ans.grades->>v_i)::numeric; exception when others then v_g := null; end;
                if v_g is null then
                    return jsonb_build_object('error','یک نمره ذخیره‌شده نامعتبر است؛ ابتدا تصحیح فردی را بررسی کنید');
                end if;
            else
                v_resp := case when jsonb_typeof(v_ans.responses)='array' then v_ans.responses->v_i else null end;
                v_g := public.native_auto_grade_with_penalty_v335_37(v_q->>'type',v_score,v_key,v_resp,v_exam.neg_marking);
                if v_g is null then
                    v_incomplete := true;
                    if not (v_i+1 = any(v_missing_q)) then v_missing_q := v_missing_q || (v_i+1); end if;
                    begin v_g := coalesce((v_ans.grades->>v_i)::numeric,0); exception when others then v_g:=0; end;
                else
                    v_objective := v_objective || v_i;
                end if;
            end if;
            if v_g is null or v_g::text in ('NaN','Infinity','-Infinity')
               or v_g < public.native_grade_min_v335_37(v_q->>'type',v_score,v_exam.neg_marking)-0.00000001
               or v_g > v_score then
                return jsonb_build_object('error',format('نمره سؤال %s خارج از بازه مجاز است',v_i+1));
            end if;
            v_grades := v_grades || to_jsonb(v_g);
            v_total := v_total + v_g;
        end loop;
        if v_incomplete then v_missing_answers:=v_missing_answers+1; end if;
        v_pending := v_pending || jsonb_build_array(jsonb_build_object(
          'id',v_ans.id,'grades',v_grades,'total',greatest(v_total,0),'objective',to_jsonb(v_objective)));
    end loop;
    if v_missing_answers>0 then
      return jsonb_build_object('error','سؤال‌های '||array_to_string(v_missing_q,'، ')||' هنوز نمره ندارند؛ ابتدا تصحیح و ذخیره کنید.',
        'incomplete_answers',v_missing_answers,'missing_questions',to_jsonb(v_missing_q));
    end if;
    for v_item in select value from jsonb_array_elements(v_pending) loop
      update public.answers set grades=v_item->'grades',total_grade=(v_item->>'total')::numeric,
        graded=true,auto_graded=false,graded_at=now()::text where id=v_item->>'id' and exam_id=p_exam;
      insert into public.native_grade_progress(answer_id,question_index,teacher_id,updated_at)
      select v_item->>'id',i::integer,v_uid,now() from jsonb_array_elements_text(v_item->'objective') as i
      on conflict(answer_id,question_index) do update set updated_at=excluded.updated_at;
      v_count:=v_count+1;
    end loop;
    return jsonb_build_object('ok',true,'finalized',v_count);
end;
$$;

create or replace function public.exam_autograde_info(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb := '[]'::jsonb;
    v_key jsonb;
    v_total integer := 0;
    v_desc integer := 0;
    v_pending integer := 0;
    v_auto integer := 0;
    v_i integer;
    v_q jsonb;
    v_auto_construct boolean;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid limit 1;
    if v_exam.id is null then return jsonb_build_object('error', 'آزمون پیدا نشد یا متعلق به این حساب نیست'); end if;
    select coalesce(k.answers, '[]'::jsonb) into v_keys from public.exam_keys k where k.exam_id = v_exam.id limit 1;
    v_keys := coalesce(v_keys, '[]'::jsonb);

    if jsonb_typeof(coalesce(v_exam.questions, 'null'::jsonb)) = 'array' then
        v_total := jsonb_array_length(v_exam.questions);
        for v_i in 0 .. v_total - 1 loop
            v_q := coalesce(v_exam.questions -> v_i, '{}'::jsonb);
            select coalesce(item, '{}'::jsonb) into v_key
            from jsonb_array_elements(case when jsonb_typeof(v_keys) = 'array' then v_keys else '[]'::jsonb end) with ordinality as key_row(item, ord)
            where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
            order by ord limit 1;
            v_key := coalesce(v_key, '{}'::jsonb);
            v_auto_construct := (jsonb_typeof(v_key -> 'constructionTargets') = 'array' and jsonb_array_length(v_key -> 'constructionTargets') > 0)
                             or (jsonb_typeof(v_key -> 'constructionConstraints') = 'array' and jsonb_array_length(v_key -> 'constructionConstraints') > 0);
            if lower(coalesce(v_q ->> 'type', '')) in ('essay', 'long', 'descriptive') and not coalesce(v_auto_construct,false) then
                v_desc := v_desc + 1;
            end if;
        end loop;
    end if;

    select count(*) filter (where coalesce(a.graded, false) = false),
           count(*) filter (where coalesce(a.auto_graded, false))
      into v_pending, v_auto
      from public.answers a where a.exam_id = v_exam.id;
    return jsonb_build_object('ok', true, 'total_q', v_total, 'desc_q', v_desc, 'partial', 0,
        'fully_auto', v_desc = 0, 'auto_only', v_pending, 'pending', v_pending, 'auto_graded', v_auto, 'ready_score', 0);
end;
$$;
revoke all on function public.native_grade_min_v335_37(text,numeric,numeric) from public, anon;
grant execute on function public.native_grade_min_v335_37(text,numeric,numeric) to authenticated;
revoke all on function public.native_question_shown_v335_37(jsonb,integer) from public, anon;
grant execute on function public.native_question_shown_v335_37(jsonb,integer) to authenticated;
revoke all on function public.native_auto_grade_with_penalty_v335_37(text,numeric,jsonb,jsonb,numeric) from public, anon;
grant execute on function public.native_auto_grade_with_penalty_v335_37(text,numeric,jsonb,jsonb,numeric) to authenticated;
revoke all on function public.native_save_grade(text,jsonb,text) from public, anon;
grant execute on function public.native_save_grade(text,jsonb,text) to authenticated;
revoke all on function public.native_bulk_save_question_grades_v1(text,integer,jsonb) from public, anon;
grant execute on function public.native_bulk_save_question_grades_v1(text,integer,jsonb) to authenticated;
revoke all on function public.native_save_grade_parts_v1(text,jsonb,text,jsonb,jsonb) from public, anon;
grant execute on function public.native_save_grade_parts_v1(text,jsonb,text,jsonb,jsonb) to authenticated;
revoke all on function public.submit_answer(text,jsonb,jsonb,jsonb) from public, anon;
grant execute on function public.submit_answer(text,jsonb,jsonb,jsonb) to authenticated;
revoke all on function public.approve_auto_grades(text,text) from public, anon;
grant execute on function public.approve_auto_grades(text,text) to authenticated;
revoke all on function public.native_finalize_bulk_grades_v1(text) from public, anon;
grant execute on function public.native_finalize_bulk_grades_v1(text) to authenticated;
revoke all on function public.exam_autograde_info(text) from public, anon;
grant execute on function public.exam_autograde_info(text) to authenticated;
commit;
notify pgrst, 'reload schema';
