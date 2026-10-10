-- V255 — «۵۰ سؤال بساز، ۲۰ تا تصادفی به دانش‌آموز نشان بده».
-- ۱) exams.pick_count (۰ = همهٔ سؤال‌ها)؛ exam_attempts.shown_q = شاخص‌های نمایش‌داده‌شدهٔ همین تلاش.
-- ۲) get_exam_for_student: زیرمجموعهٔ تصادفی هنگام ساخت تلاش انتخاب و ذخیره می‌شود (ادامهٔ تلاش = همان سؤال‌ها)؛
--    فقط سؤال‌های نمایش‌داده‌شده با همان «i» اصلی برمی‌گردند؛ total_score = جمع بارم همان سؤال‌ها.
-- ۳) submit_answer: آرایهٔ فشردهٔ پاسخ‌ها (به ترتیب i صعودی، همان قرارداد اپ/سایت) به آرایهٔ کامل
--    هم‌طول با سؤال‌های آزمون نگاشت می‌شود تا تصحیح/approve_auto_grades بدون تغییر کار کند؛
--    meta.shown_q و meta.max_score روی answers ذخیره می‌شود.
-- ۴) native_my_answers_v1 / native_my_answer_detail_v1: «از N» = meta.max_score و فقط سؤال‌های نمایش‌داده‌شده.
-- ۵) native_save_exam_v2: ذخیرهٔ pick_count.
begin;

alter table public.exams add column if not exists pick_count integer not null default 0;
alter table public.exam_attempts add column if not exists shown_q jsonb;

-- ------------------------------------------------------------
-- get_exam_for_student (بدنهٔ قبلی عیناً + انتخاب زیرمجموعه)
-- ------------------------------------------------------------
create or replace function public.get_exam_for_student(p_code text)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_exam exams%rowtype;
  v_name text;
  v_allowed boolean;
  v_qs jsonb;
  v_st record;
  v_no int;
  v_exp timestamptz;
  v_att exam_attempts%rowtype;
  v_pick int;
  v_n int;
  v_shown jsonb := null;
  v_total numeric;
begin
  select * into v_exam from exams where upper(code) = upper(p_code);
  if not found then return jsonb_build_object('error','آزمون با این کد یافت نشد'); end if;
  if not coalesce(v_exam.is_open,true) then
    return jsonb_build_object('error','این آزمون بسته است'); end if;

  if v_exam.opens_at is not null and now() < v_exam.opens_at then
    return jsonb_build_object('error','آزمون هنوز شروع نشده است'); end if;
  if v_exam.closes_at is not null and now() > v_exam.closes_at then
    return jsonb_build_object('error','مهلت آزمون به پایان رسیده است'); end if;

  select full_name into v_name from profiles where id = auth.uid();
  if v_name is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;

  select (count(*) = 0) or bool_or(student_name = v_name)
    into v_allowed from class_students where teacher_id = v_exam.teacher_id;
  if not coalesce(v_allowed,true) then
    return jsonb_build_object('error','شما در لیست دانش‌آموزان این معلم نیستید'); end if;

  -- ---------- وضعیت تلاش‌ها ----------
  select * into v_st from attempt_state(v_exam.id, auth.uid());

  if not v_st.can_start then
    return jsonb_build_object('error', v_st.reason,
      'attempts_used', v_st.used, 'attempts_allowed', v_st.max_allowed,
      'cooldown_left', v_st.cooldown_left);
  end if;

  -- ---------- تلاش باز را ادامه بده، وگرنه تازه بساز ----------
  if v_st.open_no is not null then
    v_no  := v_st.open_no;
    v_exp := v_st.open_expires;
    update exam_attempts set started_at = started_at   -- بدون تغییر
     where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
  else
    update exam_attempts
       set abandoned = true
     where exam_id = v_exam.id and student_id = auth.uid()
       and submitted_at is null and abandoned = false
       and expires_at is not null and expires_at <= now();

    select coalesce(max(attempt_no), 0) + 1 into v_no
      from exam_attempts where exam_id = v_exam.id and student_id = auth.uid();

    v_exp := case when v_exam.duration is not null
                  then now() + make_interval(mins => v_exam.duration) end;

    insert into exam_attempts
      (exam_id, student_id, student_name, attempt_no, started_at, expires_at)
    values (v_exam.id, auth.uid(), v_name, v_no, now(), v_exp)
    on conflict (exam_id, student_id, attempt_no) do update
      set started_at = now(), expires_at = excluded.expires_at,
          abandoned = false, shown_q = null
    returning * into v_att;
    v_exp := v_att.expires_at;
  end if;

  -- ---------- exam_sessions را هم‌گام نگه دار (کد قدیمی) ----------
  insert into exam_sessions (exam_id, student_id, started_at, expires_at)
  values (v_exam.id, auth.uid(), now(), v_exp)
  on conflict (exam_id, student_id) do update
    set started_at = now(), expires_at = excluded.expires_at;

  -- ---------- V255: زیرمجموعهٔ تصادفی این تلاش ----------
  v_n := coalesce(jsonb_array_length(coalesce(v_exam.questions,'[]'::jsonb)), 0);
  v_pick := coalesce(v_exam.pick_count, 0);
  if v_pick > 0 and v_pick < v_n then
    select shown_q into v_shown from exam_attempts
     where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
    if v_shown is null or jsonb_typeof(v_shown) <> 'array' or jsonb_array_length(v_shown) = 0 then
      select coalesce(jsonb_agg(s.idx order by s.idx), '[]'::jsonb) into v_shown
        from (select g - 1 as idx from generate_series(1, v_n) g order by random() limit v_pick) s;
      update exam_attempts set shown_q = v_shown
       where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
    end if;
  end if;

  -- ---------- حذف کلید پاسخ (فقط سؤال‌های نمایش‌داده‌شده، با i اصلی) ----------
  select coalesce(jsonb_agg(
           (q - 'correctOption' - 'correctAnswer' - 'accept' - 'answer'
              - 'tolerance' - 'explanation')
           || jsonb_build_object('i', ord - 1) order by ord), '[]'::jsonb),
         coalesce(sum(coalesce((q->>'score')::numeric, 0)), 0)
    into v_qs, v_total
    from jsonb_array_elements(coalesce(v_exam.questions,'[]'::jsonb)) with ordinality as t(q, ord)
   where v_shown is null or v_shown @> to_jsonb((ord - 1)::int);

  return jsonb_build_object(
    'id', v_exam.id, 'title', v_exam.title, 'subject', v_exam.subject,
    'duration', v_exam.duration,
    'total_score', case when v_shown is null then v_exam.total_score else v_total end,
    'code', v_exam.code,
    'shuffle_q', coalesce(v_exam.shuffle_q,false),
    'shuffle_opt', coalesce(v_exam.shuffle_opt,false),
    'questions', v_qs,
    'expires_at', v_exp,
    'attempts_allowed', v_st.max_allowed,
    'attempts_used', v_st.used,
    'attempt_no', v_no,
    'resumed', (v_st.open_no is not null),
    'grade_policy', coalesce(v_exam.grade_policy,'last'),
    'pick_count', v_pick,
    'question_total', v_n,
    'shown_q', v_shown,
    'server_now', now());
end $function$;

-- ------------------------------------------------------------
-- submit_answer (بدنهٔ قبلی عیناً + نگاشت پاسخ‌های فشرده به آرایهٔ کامل)
-- ------------------------------------------------------------
create or replace function public.submit_answer(p_exam_id text, p_responses jsonb, p_images jsonb default '{}'::jsonb, p_meta jsonb default '{}'::jsonb)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_exam exams%rowtype;
  v_key jsonb; v_name text;
  v_grades jsonb := '[]'::jsonb;
  v_total numeric := 0;
  v_q jsonb; v_i int; v_score numeric; v_resp text; v_ok boolean; v_g numeric;
  v_id text; v_acc jsonb; v_num numeric; v_tol numeric;
  v_att exam_attempts%rowtype;
  v_st record;
  v_no int;
  v_shown jsonb := null;
  v_full jsonb;
  v_pos int;
  v_n int;
  v_max numeric := 0;
  v_meta jsonb;
begin
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
            case when v_exam.duration is not null
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

  -- ---------- V255: نگاشت پاسخ‌های فشرده به آرایهٔ کامل ----------
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
    v_full := p_responses;
    if v_shown is not null and jsonb_typeof(v_shown) <> 'array' then v_shown := null; end if;
    if v_shown is not null and jsonb_array_length(v_shown) = 0 then v_shown := null; end if;
  end if;

  select answers into v_key from exam_keys where exam_id = p_exam_id;

  -- ---------- نمره‌دهی خودکار (فقط سؤال‌های نمایش‌داده‌شده) ----------
  v_i := 0;
  for v_q in select * from jsonb_array_elements(coalesce(v_exam.questions,'[]'::jsonb))
  loop
    v_score := coalesce((v_q->>'score')::numeric, 0);
    v_resp  := v_full->>(v_i::text);
    v_g     := 0;
    v_ok    := false;

    if v_shown is null or v_shown @> to_jsonb(v_i) then
      v_max := v_max + v_score;
      if v_key is not null and v_resp is not null and v_resp <> '' then
        case v_q->>'type'
          when 'multiple' then
            v_ok := (v_resp = (v_key->>(v_i::text)));
          when 'truefalse' then
            v_ok := (lower(v_resp) = lower(coalesce(v_key->>(v_i::text),'')));
          when 'fill' then
            v_acc := coalesce(v_key->(v_i::text), '[]'::jsonb);
            if jsonb_typeof(v_acc) = 'array' then
              v_ok := exists (
                select 1 from jsonb_array_elements_text(v_acc) a
                 where lower(btrim(a)) = lower(btrim(v_resp)));
            else
              v_ok := lower(btrim(v_resp)) = lower(btrim(coalesce(v_acc #>> '{}','')));
            end if;
          when 'numeric' then
            begin
              v_num := v_resp::numeric;
              v_tol := coalesce((v_q->>'tolerance')::numeric, 0);
              v_ok  := abs(v_num - coalesce((v_key->>(v_i::text))::numeric, 0)) <= v_tol;
            exception when others then v_ok := false;
            end;
          when 'matching' then
            v_ok := (v_resp::jsonb = coalesce(v_key->(v_i::text), '{}'::jsonb));
          else
            v_ok := false;
        end case;

        if v_ok then
          v_g := v_score;
        elsif coalesce(v_exam.neg_marking,0) > 0
              and (v_q->>'type') in ('multiple','truefalse') then
          v_g := -1 * coalesce(v_exam.neg_marking,0) * v_score;
        end if;
      end if;
    end if;

    v_grades := v_grades || to_jsonb(v_g);
    v_total := v_total + v_g;
    v_i := v_i + 1;
  end loop;

  v_meta := coalesce(p_meta,'{}'::jsonb);
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

create or replace function public.native_answer_max_score_v255(p_meta jsonb)
returns numeric
language sql
immutable
as $$
    select case
        when p_meta is null or jsonb_typeof(p_meta) <> 'object' then null
        when (p_meta->>'max_score') ~ '^[0-9]+(\.[0-9]+)?$' then (p_meta->>'max_score')::numeric
        else null end;
$$;
revoke all on function public.native_answer_max_score_v255(jsonb) from public, anon;
grant execute on function public.native_answer_max_score_v255(jsonb) to authenticated;

-- ------------------------------------------------------------
-- کارنامهٔ دانش‌آموز: «از N» = meta.max_score (اگر باشد)
-- ------------------------------------------------------------
create or replace function public.native_my_answers_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select jsonb_build_object(
        'ok', true,
        'items', coalesce(jsonb_agg(
            jsonb_build_object(
                'id', a.id::text,
                'exam_id', a.exam_id,
                'title', coalesce(e.title, 'آزمون'),
                'subject', coalesce(e.subject, ''),
                'submitted_at', a.submitted_at,
                'graded', coalesce(a.graded, false),
                'total_grade', coalesce(a.total_grade, 0),
                'total_score', coalesce(public.native_answer_max_score_v255(a.meta), e.total_score, 0),
                'feedback', coalesce(a.feedback, '')
            ) order by a.submitted_at desc
        ), '[]'::jsonb)
    )
    from public.answers a
    join public.exams e on e.id = a.exam_id
    where auth.uid() is not null
      and a.student_id = auth.uid();
$$;


create or replace function public.native_my_answer_detail_v1(p_answer text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_answer public.answers%rowtype;
    v_exam public.exams%rowtype;
    v_keys jsonb := '[]'::jsonb;
    v_questions jsonb := '[]'::jsonb;
    v_responses jsonb := '[]'::jsonb;
    v_grades jsonb := '[]'::jsonb;
    v_question jsonb;
    v_key jsonb;
    v_index integer;
    v_shown jsonb := null;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;

    select * into v_answer
    from public.answers a
    where a.id::text = btrim(coalesce(p_answer, ''))
      and a.student_id = v_uid;
    if v_answer.id is null then
        return jsonb_build_object('error', 'پاسخ پیدا نشد یا متعلق به این حساب نیست');
    end if;

    select * into v_exam from public.exams e where e.id = v_answer.exam_id;
    if v_exam.id is null then
        return jsonb_build_object('error', 'آزمون پاسخ پیدا نشد');
    end if;

    if coalesce(v_answer.graded, false) then
        select coalesce(k.answers, '[]'::jsonb) into v_keys
        from public.exam_keys k
        where k.exam_id = v_exam.id;
        v_keys := coalesce(v_keys, '[]'::jsonb);
    end if;

    -- V255: فقط سؤال‌های نمایش‌داده‌شده (meta.shown_q)؛ پاسخ/نمره هم به همان ترتیب فشرده می‌شوند.
    if jsonb_typeof(v_answer.meta) = 'object' and jsonb_typeof(v_answer.meta->'shown_q') = 'array'
       and jsonb_array_length(v_answer.meta->'shown_q') > 0 then
        v_shown := v_answer.meta->'shown_q';
    end if;

    if jsonb_typeof(coalesce(v_exam.questions, '[]'::jsonb)) = 'array' then
        for v_index in 0 .. jsonb_array_length(coalesce(v_exam.questions, '[]'::jsonb)) - 1 loop
            if v_shown is not null and not (v_shown @> to_jsonb(v_index)) then
                continue;
            end if;
            v_question := coalesce(v_exam.questions -> v_index, '{}'::jsonb)
                - 'correctOption' - 'correctAnswer' - 'accept' - 'answer'
                - 'tolerance' - 'matchAnswer' - 'explanation';
            v_key := '{}'::jsonb;
            if coalesce(v_answer.graded, false) then
                select coalesce(item, '{}'::jsonb) into v_key
                from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
                where coalesce((item->>'i')::integer, (ord - 1)::integer) = v_index
                order by ord
                limit 1;
                v_key := coalesce(v_key, '{}'::jsonb) - 'i';
            end if;
            v_questions := v_questions || jsonb_build_array(v_question || v_key || jsonb_build_object('i', v_index));
            v_responses := v_responses || jsonb_build_array(coalesce(v_answer.responses -> v_index, '""'::jsonb));
            v_grades := v_grades || jsonb_build_array(coalesce(v_answer.grades -> v_index, '0'::jsonb));
        end loop;
    end if;

    return jsonb_build_object(
        'ok', true,
        'id', v_answer.id::text,
        'exam_id', v_exam.id,
        'title', coalesce(v_exam.title, 'آزمون'),
        'subject', coalesce(v_exam.subject, ''),
        'graded', coalesce(v_answer.graded, false),
        'total_grade', coalesce(v_answer.total_grade, 0),
        'total_score', coalesce(public.native_answer_max_score_v255(v_answer.meta), v_exam.total_score, 0),
        'feedback', coalesce(v_answer.feedback, ''),
        'submitted_at', v_answer.submitted_at,
        'questions', v_questions,
        'responses', v_responses,
        'response_images', coalesce(v_answer.response_images, '{}'::jsonb),
        'grades', v_grades,
        'shown_q', v_shown
    );
end;
$$;

-- ------------------------------------------------------------
-- native_save_exam_v2 (بدنهٔ V61 عیناً + ذخیرهٔ pick_count)
-- ------------------------------------------------------------
create or replace function public.native_save_exam_v2(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_opens timestamptz;
    v_closes timestamptz;
    v_result jsonb;
    v_exam text;
    v_count integer;
    v_payload jsonb := p_payload;
    v_schools jsonb := '[]'::jsonb;
    v_students jsonb;
begin
    -- V255: pick_count نامعتبر (غیرعددی) → خطای فارسی.
    if coalesce(v_payload->>'pick_count','') <> '' and (v_payload->>'pick_count') !~ '^[0-9]+$' then
        return jsonb_build_object('error','تعداد سؤال‌های تصادفی باید عدد باشد');
    end if;
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
        return jsonb_build_object('error','داده آزمون نامعتبر است');
    end if;
    begin
        v_opens := nullif(btrim(coalesce(p_payload->>'opens_at','')), '')::timestamptz;
        v_closes := nullif(btrim(coalesce(p_payload->>'closes_at','')), '')::timestamptz;
    exception when others then
        return jsonb_build_object('error','زمان بازشدن یا پایان معتبر نیست');
    end;
    if v_opens is not null and v_closes is not null and v_closes <= v_opens then
        return jsonb_build_object('error','مهلت پایان باید بعد از زمان بازشدن باشد');
    end if;

    -- V61.0 — مخاطب «مدارس»: گسترش به دانش‌آموزان ثبت‌شدهٔ همان مدرسه‌ها.
    if coalesce(p_payload->>'audience','all') = 'schools' then
        v_schools := coalesce(p_payload->'schools','[]'::jsonb);
        if coalesce(jsonb_array_length(v_schools),0) = 0 then
            return jsonb_build_object('error','حداقل یک مدرسه انتخاب کنید');
        end if;
        if exists (
            select 1 from jsonb_array_elements_text(v_schools) sc(id)
            where not exists (
                select 1 from public.school_memberships m
                where m.school_id = sc.id::uuid and m.user_id = v_uid and m.status = 'active'
            )
        ) then
            return jsonb_build_object('error','یک مدرسه انتخابی متعلق به این معلم نیست');
        end if;
        v_students := public.native_exam_school_students_v61(v_schools);
        if coalesce(jsonb_array_length(v_students),0) = 0 then
            return jsonb_build_object('error','مدرسه انتخابی دانش‌آموز ثبت‌شده ندارد');
        end if;
        v_payload := p_payload || jsonb_build_object('audience','students','students',v_students);
    end if;

    -- گسترش school ممکن است دانش‌آموزِ معلم دیگر را شامل شود که v1 رد می‌کند؛
    -- برای این حالت اعتبارسنجی/درج مخاطبان بعد از v1 با قواعد V61 انجام می‌شود.
    if coalesce(v_payload->>'audience','all') = 'students'
       and not public.native_exam_audience_students_ok_v61(v_payload->'students') then
        return jsonb_build_object('error','دانش‌آموز مخاطب نامعتبر است');
    end if;

    v_result := public.native_save_exam_v1(
        case when coalesce(v_payload->>'audience','all') = 'students'
             then v_payload || jsonb_build_object('audience','all') || jsonb_build_object('students','[]'::jsonb)
             else v_payload end
    );
    if v_result ? 'error' then return v_result; end if;
    v_exam := coalesce(v_result->>'id', v_payload->>'id');

    -- درج مخاطبان students با قواعد V61 (مالک یا هم‌مدرسه‌ای یا لینک‌شده).
    if coalesce(v_payload->>'audience','all') = 'students' then
        update public.exams set audience = 'students' where id = v_exam and teacher_id = v_uid;
        delete from public.exam_audience_students where exam_id = v_exam;
        insert into public.exam_audience_students(exam_id, student_id)
        select distinct v_exam, w.id::uuid
        from jsonb_array_elements_text(v_payload->'students') w(id);
    end if;
    delete from public.exam_audience_schools where exam_id = v_exam;
    if coalesce(jsonb_array_length(v_schools),0) > 0 then
        insert into public.exam_audience_schools(exam_id, school_id)
        select distinct v_exam, sc.id::uuid from jsonb_array_elements_text(v_schools) sc(id);
    end if;

    update public.exams
    set opens_at = v_opens,
        closes_at = v_closes,
        -- V255 — «پیک N از کل»: صفر یعنی همهٔ سؤال‌ها.
        pick_count = greatest(0, coalesce(nullif(btrim(coalesce(v_payload->>'pick_count','')), '')::integer, 0))
    where id = v_exam and teacher_id = v_uid;
    get diagnostics v_count = row_count;
    if v_count <> 1 then raise exception 'ذخیره زمان‌بندی آزمون کامل نشد'; end if;
    return v_result || jsonb_build_object('opens_at',v_opens,'closes_at',v_closes);
end;
$$;
revoke all on function public.native_save_exam_v2(jsonb) from public, anon;
grant execute on function public.native_save_exam_v2(jsonb) to authenticated;

commit;
