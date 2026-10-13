-- V272 — «۵۰ سؤال بساز، ۲۰ تا تصادفی» با بخش‌بندی بر اساس نوع سؤال.
-- exams.pick_rules jsonb = {"multiple": 10, "essay": 2, ...}: از هر نوعِ دارای قاعده، N سؤال تصادفی برای هر دانش‌آموز؛
-- نوعِ بدون قاعده = همهٔ سؤال‌هایش. اگر pick_rules خالی باشد، pick_count (V255) مثل قبل کار می‌کند.
-- زیرمجموعه در exam_attempts.shown_q ثابت می‌ماند (ورود دوباره = همان سؤال‌ها)؛ نمره از سؤال‌های نمایش‌داده‌شده (قرارداد V255).
-- فایل get_exam_for_student = بدنهٔ V260.7 عیناً + بلوک انتخاب؛ native_save_exam_v2 = بدنهٔ V255 عیناً + ذخیرهٔ pick_rules.
begin;
alter table public.exams add column if not exists pick_rules jsonb not null default '{}'::jsonb;

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
  v_rules jsonb := '{}'::jsonb; -- V272
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

  -- V260.7 — دروازهٔ مخاطبان: اگر آزمون برای کلاس/دانش‌آموزان/مدرسهٔ خاصی تعریف شده، فقط همان‌ها با کد وارد می‌شوند
  -- (قبلاً مخاطب فقط برای اعلان‌ها به کار می‌رفت و هر دانش‌آموزی با داشتن کد می‌توانست شرکت کند).
  if coalesce(v_exam.audience,'all') <> 'all' then
    select (
      (v_exam.audience = 'students' and exists (
         select 1 from public.exam_audience_students s where s.exam_id = v_exam.id and s.student_id = auth.uid()))
      or (v_exam.audience = 'classes' and exists (
         select 1 from public.exam_audience_classes ac join public.class_members m on m.class_id = ac.class_id
         where ac.exam_id = v_exam.id and m.student_id = auth.uid()))
      or (v_exam.audience = 'schools' and exists (
         select 1 from public.exam_audience_schools es join public.school_students ss on ss.school_id = es.school_id
         where es.exam_id = v_exam.id and ss.student_id = auth.uid()))
    ) into v_allowed;
    if not coalesce(v_allowed,false) then
      return jsonb_build_object('error','این آزمون برای کلاس یا گروه شما تعریف نشده است؛ از معلم خود بپرسید'); end if;
  end if;

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

    -- V275 — مدت ۰/خالی = بدون محدودیت (مثل اپ و سایت)؛ پیش‌تر expires_at = now() می‌شد و آزمون بلافاصله «پایان‌یافته» ارسال می‌شد
    v_exp := case when coalesce(v_exam.duration, 0) > 0
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
  v_rules := case when jsonb_typeof(v_exam.pick_rules) = 'object' then v_exam.pick_rules else '{}'::jsonb end;
  -- V272: قاعده‌های بخش‌بندی‌شده (نوع سؤال → تعداد) بر pick_count مقدم‌اند
  if v_rules <> '{}'::jsonb then
    select shown_q into v_shown from exam_attempts
     where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
    if v_shown is null or jsonb_typeof(v_shown) <> 'array' or jsonb_array_length(v_shown) = 0 then
      -- هر نوعِ دارای قاعده: N تای تصادفی از همان نوع؛ نوع بدون قاعده: همهٔ سؤال‌هایش
      with q as (
        select ord - 1 as idx,
               lower(coalesce(t.q->>'type', '')) as typ,
               row_number() over (partition by lower(coalesce(t.q->>'type', '')) order by random()) as rn
          from jsonb_array_elements(coalesce(v_exam.questions,'[]'::jsonb)) with ordinality as t(q, ord)
      )
      select coalesce(jsonb_agg(q.idx order by q.idx), '[]'::jsonb) into v_shown
        from q
       where (v_rules ? q.typ) = false
          or q.rn <= greatest(0, coalesce(nullif(btrim(v_rules->>q.typ), '')::integer, 0));
      if jsonb_array_length(v_shown) = 0 or jsonb_array_length(v_shown) >= v_n then v_shown := null;
      else
        update exam_attempts set shown_q = v_shown
         where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
      end if;
    end if;
  elsif v_pick > 0 and v_pick < v_n then
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
    'pick_rules', v_rules,
    'question_total', v_n,
    'shown_q', v_shown,
    'server_now', now());
end $function$;

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
        pick_count = greatest(0, coalesce(nullif(btrim(coalesce(v_payload->>'pick_count','')), '')::integer, 0)),
        -- V272 — قاعده‌های بخش‌بندی‌شده: {"multiple": 10, "essay": 2} (فقط مقدارهای عددی مثبت نگه داشته می‌شوند)
        pick_rules = coalesce((
            select jsonb_object_agg(k, v::integer)
              from jsonb_each_text(case when jsonb_typeof(v_payload->'pick_rules') = 'object' then v_payload->'pick_rules' else '{}'::jsonb end) as r(k, v)
             where v ~ '^[0-9]+$' and v::integer > 0
        ), '{}'::jsonb)
    where id = v_exam and teacher_id = v_uid;
    get diagnostics v_count = row_count;
    if v_count <> 1 then raise exception 'ذخیره زمان‌بندی آزمون کامل نشد'; end if;
    return v_result || jsonb_build_object('opens_at',v_opens,'closes_at',v_closes);
end;
$$;
revoke all on function public.native_save_exam_v2(jsonb) from public, anon;
grant execute on function public.native_save_exam_v2(jsonb) to authenticated;

commit;
