-- V332.0 — تکمیل ارزیابی دورهٔ متوسطه و هم‌ترازی کامل سرور، اپ اندروید و وب:
-- ۱) زمان‌بندی بخش‌ها با اعتبارسنجی سرور (exam_attempts.section_deadlines + section_locked_responses + RPC همگام‌سازی بخش).
-- ۲) پاک‌سازی کامل کلیدهای محرمانه (rubric, partRubrics, constructionTargets, constructionConstraints) در get_exam_for_student و کارنامه.
-- ۳) نمره‌دهی خودکار شرط‌های هندسی (فاصله، برابری طول، زاویه، هم‌خطی، تعامد، توازی) + نقاط هدف در native_auto_grade_question_v1.
-- ۴) رفع باگ اندیس‌گذاری آرایهٔ JSON در submit_answer و اعمال قفل سرور برای بخش‌های منقضی‌شده.
-- ۵) برگرداندن part_grades در کنار part_feedback در native_my_answer_detail_parts_v1 و پشتیبانی approve_auto_grades / exam_autograde_info از ترسیم‌های خودکار.

begin;

alter table public.exam_attempts
  add column if not exists section_deadlines jsonb not null default '{}'::jsonb,
  add column if not exists section_locked_responses jsonb not null default '{}'::jsonb;

-- ------------------------------------------------------------
-- ۱) همگام‌سازی سرور برای شروع و قفل زمان‌بندی هر بخش آزمون
-- ------------------------------------------------------------
create or replace function public.native_sync_exam_section_v1(
  p_exam text,
  p_section text,
  p_lock boolean default false,
  p_section_responses jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_exam public.exams%rowtype;
  v_att public.exam_attempts%rowtype;
  v_sec text := btrim(coalesce(p_section, ''));
  v_minutes integer := 0;
  v_deadlines jsonb;
  v_locked jsonb;
  v_now timestamptz := now();
  v_dl timestamptz;
  v_expired jsonb := '[]'::jsonb;
begin
  if v_uid is null then
    return jsonb_build_object('error', 'ابتدا وارد شوید');
  end if;
  if v_sec = '' then
    return jsonb_build_object('error', 'نام بخش خالی است');
  end if;

  select * into v_exam from public.exams e where e.id = btrim(coalesce(p_exam, ''));
  if v_exam.id is null then
    return jsonb_build_object('error', 'آزمون یافت نشد');
  end if;

  select * into v_att
  from public.exam_attempts a
  where a.exam_id = v_exam.id
    and a.student_id = v_uid
    and a.submitted_at is null
    and coalesce(a.abandoned, false) = false
  order by a.attempt_no desc
  limit 1;

  if v_att.id is null then
    return jsonb_build_object('error', 'تلاش فعالی برای این آزمون یافت نشد');
  end if;

  begin
    v_minutes := greatest(
      0,
      least(
        1440,
        coalesce((v_exam.options -> 'sectionDurations' ->> v_sec)::numeric::integer, 0)
      )
    );
  exception when others then
    v_minutes := 0;
  end;

  v_deadlines := case
    when jsonb_typeof(v_att.section_deadlines) = 'object' then v_att.section_deadlines
    else '{}'::jsonb
  end;
  v_locked := case
    when jsonb_typeof(v_att.section_locked_responses) = 'object' then v_att.section_locked_responses
    else '{}'::jsonb
  end;

  -- ثبت اولین مهلت سرور برای این بخش (غیرقابل تمدید با رفرش صفحه یا تغییر دستگاه)
  if v_minutes > 0 and not (v_deadlines ? v_sec) then
    v_dl := v_now + make_interval(mins => v_minutes);
    if v_att.expires_at is not null and v_att.expires_at < v_dl then
      v_dl := v_att.expires_at;
    end if;
    v_deadlines := v_deadlines || jsonb_build_object(v_sec, v_dl);
  end if;

  -- قفل قطعی پاسخ‌های بخش هنگام اتمام زمان (فقط اولین تصویر ثبت می‌شود)
  if coalesce(p_lock, false) then
    if v_deadlines ? v_sec then
      begin
        v_dl := (v_deadlines ->> v_sec)::timestamptz;
        if v_dl > v_now then
          v_deadlines := v_deadlines || jsonb_build_object(v_sec, v_now);
        end if;
      exception when others then
        v_deadlines := v_deadlines || jsonb_build_object(v_sec, v_now);
      end;
    else
      v_deadlines := v_deadlines || jsonb_build_object(v_sec, v_now);
    end if;

    if not (v_locked ? v_sec) and jsonb_typeof(coalesce(p_section_responses, '{}'::jsonb)) = 'object' then
      v_locked := v_locked || jsonb_build_object(v_sec, coalesce(p_section_responses, '{}'::jsonb));
    end if;
  end if;

  update public.exam_attempts
  set section_deadlines = v_deadlines,
      section_locked_responses = v_locked
  where id = v_att.id;

  select coalesce(jsonb_agg(distinct s.sec_name), '[]'::jsonb)
    into v_expired
    from (
      select d.key as sec_name
      from jsonb_each_text(v_deadlines) as d(key, val)
      where val is not null and val::timestamptz <= v_now
      union
      select l.key as sec_name
      from jsonb_object_keys(v_locked) as l(key)
    ) s;

  return jsonb_build_object(
    'ok', true,
    'server_now', v_now,
    'section_deadlines', v_deadlines,
    'expired_sections', v_expired
  );
end;
$$;

revoke all on function public.native_sync_exam_section_v1(text, text, boolean, jsonb) from public, anon;
grant execute on function public.native_sync_exam_section_v1(text, text, boolean, jsonb) to authenticated;

-- ------------------------------------------------------------
-- ۲) get_exam_for_student: پاک‌سازی کامل کلیدها + بازگرداندن وضعیت زمان‌بندی بخش‌ها
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
  v_rules jsonb := '{}'::jsonb;
  v_total numeric;
  v_sec_deadlines jsonb := '{}'::jsonb;
  v_sec_locked jsonb := '{}'::jsonb;
  v_expired_sections jsonb := '[]'::jsonb;
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

  select * into v_st from attempt_state(v_exam.id, auth.uid());

  if not v_st.can_start then
    return jsonb_build_object('error', v_st.reason,
      'attempts_used', v_st.used, 'attempts_allowed', v_st.max_allowed,
      'cooldown_left', v_st.cooldown_left);
  end if;

  if v_st.open_no is not null then
    v_no  := v_st.open_no;
    v_exp := v_st.open_expires;
    select * into v_att
      from exam_attempts
     where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
  else
    update exam_attempts
       set abandoned = true
     where exam_id = v_exam.id and student_id = auth.uid()
       and submitted_at is null and abandoned = false
       and expires_at is not null and expires_at <= now();

    select coalesce(max(attempt_no), 0) + 1 into v_no
      from exam_attempts where exam_id = v_exam.id and student_id = auth.uid();

    v_exp := case when coalesce(v_exam.duration, 0) > 0
                  then now() + make_interval(mins => v_exam.duration) end;

    insert into exam_attempts
      (exam_id, student_id, student_name, attempt_no, started_at, expires_at, section_deadlines, section_locked_responses)
    values (v_exam.id, auth.uid(), v_name, v_no, now(), v_exp, '{}'::jsonb, '{}'::jsonb)
    on conflict (exam_id, student_id, attempt_no) do update
      set started_at = now(), expires_at = excluded.expires_at,
          abandoned = false, shown_q = null,
          section_deadlines = '{}'::jsonb, section_locked_responses = '{}'::jsonb
    returning * into v_att;
    v_exp := v_att.expires_at;
  end if;

  insert into exam_sessions (exam_id, student_id, started_at, expires_at)
  values (v_exam.id, auth.uid(), now(), v_exp)
  on conflict (exam_id, student_id) do update
    set started_at = now(), expires_at = excluded.expires_at;

  v_sec_deadlines := case when jsonb_typeof(v_att.section_deadlines) = 'object' then v_att.section_deadlines else '{}'::jsonb end;
  v_sec_locked := case when jsonb_typeof(v_att.section_locked_responses) = 'object' then v_att.section_locked_responses else '{}'::jsonb end;

  select coalesce(jsonb_agg(distinct s.sec_name), '[]'::jsonb)
    into v_expired_sections
    from (
      select d.key as sec_name
      from jsonb_each_text(v_sec_deadlines) as d(key, val)
      where val is not null and val::timestamptz <= now()
      union
      select l.key as sec_name
      from jsonb_object_keys(v_sec_locked) as l(key)
    ) s;

  v_n := coalesce(jsonb_array_length(coalesce(v_exam.questions,'[]'::jsonb)), 0);
  v_pick := coalesce(v_exam.pick_count, 0);
  v_rules := case when jsonb_typeof(v_exam.pick_rules) = 'object' then v_exam.pick_rules else '{}'::jsonb end;
  if v_rules <> '{}'::jsonb then
    select shown_q into v_shown from exam_attempts
     where exam_id = v_exam.id and student_id = auth.uid() and attempt_no = v_no;
    if v_shown is null or jsonb_typeof(v_shown) <> 'array' or jsonb_array_length(v_shown) = 0 then
      with base as (
        select ord - 1 as idx,
               lower(coalesce(t.q->>'type', '')) as typ,
               nullif(btrim(t.q->>'topic'), '') as topic,
               nullif(btrim(t.q->>'sectionTitle'), '') as section_title
          from jsonb_array_elements(coalesce(v_exam.questions,'[]'::jsonb)) with ordinality as t(q, ord)
      ), keyed as (
        select b.idx, b.typ,
               case
                 when b.topic is not null and v_rules ? ('topic:' || b.topic) then 'topic:' || b.topic
                 when b.section_title is not null and v_rules ? ('section:' || b.section_title) then 'section:' || b.section_title
                 else b.typ
               end as rule_key
          from base b
      ), ranked as (
        select k.idx, k.rule_key,
               row_number() over (partition by k.rule_key order by random()) as rn
          from keyed k
      )
      select coalesce(jsonb_agg(r.idx order by r.idx), '[]'::jsonb) into v_shown
        from ranked r
       where (v_rules ? r.rule_key) = false
          or r.rn <= greatest(0, coalesce(nullif(btrim(v_rules->>r.rule_key), '')::integer, 0));
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

  select coalesce(jsonb_agg(
           (q - 'correctOption' - 'correctIndex' - 'correctAnswer' - 'accept' - 'answer'
              - 'tolerance' - 'matchAnswer' - 'explanation' - 'rubric' - 'partRubrics'
              - 'constructionTargets' - 'constructionConstraints')
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
    'options', coalesce(v_exam.options, '{}'::jsonb),
    'section_deadlines', v_sec_deadlines,
    'expired_sections', v_expired_sections,
    'question_total', v_n,
    'shown_q', v_shown,
    'server_now', now());
end $function$;

revoke all on function public.get_exam_for_student(text) from public, anon;
grant execute on function public.get_exam_for_student(text) to authenticated;

-- ------------------------------------------------------------
-- ۳) native_auto_grade_question_v1: نقاط هدف + ۶ شرط هندسی (distance, equal_length, angle, collinear, perpendicular, parallel)
-- ------------------------------------------------------------
create or replace function public.native_auto_grade_question_v1(p_type text, p_score numeric, p_key jsonb, p_resp jsonb)
returns numeric
language plpgsql
immutable
as $$
declare
    v_type text := lower(coalesce(p_type, ''));
    v_resp_text text;
    v_num numeric;
    v_ans numeric;
    v_tol numeric;
    v_ok integer := 0;
    v_n integer := 0;
    v_k text;
    v_acc jsonb;
    v_item jsonb;
    v_cs boolean;
    v_targets jsonb;
    v_constraints jsonb;
    v_has_targets boolean;
    v_has_constraints boolean;
    v_pointset jsonb;
    v_target jsonb;
    v_constraint jsonb;
    v_actual jsonb;
    v_label text;
    v_ctype text;
    v_pts jsonb;
    v_target_x numeric;
    v_target_y numeric;
    v_actual_x numeric;
    v_actual_y numeric;
    v_ax numeric; v_ay numeric;
    v_bx numeric; v_by numeric;
    v_cx numeric; v_cy numeric;
    v_dx numeric; v_dy numeric;
    v_ux numeric; v_uy numeric;
    v_vx numeric; v_vy numeric;
    v_nu numeric; v_nv numeric;
    v_dot numeric;
    v_deg numeric;
    v_dist1 numeric;
    v_dist2 numeric;
    v_cval numeric;
    v_ctol numeric;
    v_cweight numeric;
    v_cok boolean;
    v_point_tol numeric;
    v_target_score numeric := 0;
    v_earned_score numeric := 0;
begin
    v_targets := coalesce(p_key -> 'constructionTargets', '[]'::jsonb);
    v_constraints := coalesce(p_key -> 'constructionConstraints', '[]'::jsonb);
    v_has_targets := jsonb_typeof(v_targets) = 'array' and jsonb_array_length(v_targets) > 0;
    v_has_constraints := jsonb_typeof(v_constraints) = 'array' and jsonb_array_length(v_constraints) > 0;

    if v_type in ('essay', 'long', 'descriptive') and not v_has_targets and not v_has_constraints then
        return null;
    end if;
    if p_resp is null or jsonb_typeof(p_resp) = 'null' or (jsonb_typeof(p_resp) = 'string' and trim(p_resp #>> '{}') = '') then
        return 0;
    end if;
    v_resp_text := p_resp #>> '{}';

    if v_type in ('essay', 'long', 'descriptive') and (v_has_targets or v_has_constraints) then
        begin
            v_pointset := v_resp_text::jsonb;
        exception when others then
            return 0;
        end;
        if jsonb_typeof(v_pointset) <> 'object' then
            return 0;
        end if;

        if v_has_targets then
            for v_target in select value from jsonb_array_elements(v_targets) loop
                v_label := v_target ->> 'label';
                begin
                    v_target_x := (v_target ->> 'x')::numeric;
                    v_target_y := (v_target ->> 'y')::numeric;
                    v_point_tol := greatest(0, (v_target ->> 'tolerance')::numeric);
                    v_cweight := greatest(0, coalesce((v_target ->> 'score')::numeric, 0));
                    v_target_score := v_target_score + v_cweight;
                exception when others then
                    continue;
                end;
                v_actual := v_pointset -> v_label;
                if jsonb_typeof(v_actual) = 'object' then
                    begin
                        v_actual_x := (v_actual ->> 'x')::numeric;
                        v_actual_y := (v_actual ->> 'y')::numeric;
                        if sqrt(power(v_actual_x - v_target_x, 2) + power(v_actual_y - v_target_y, 2)) <= v_point_tol + 0.000001 then
                            v_earned_score := v_earned_score + v_cweight;
                        end if;
                    exception when others then null; end;
                end if;
            end loop;
        end if;

        if v_has_constraints then
            for v_constraint in select value from jsonb_array_elements(v_constraints) loop
                v_ctype := lower(coalesce(v_constraint ->> 'type', ''));
                v_pts := coalesce(v_constraint -> 'points', '[]'::jsonb);
                begin
                    v_cval := coalesce((v_constraint ->> 'value')::numeric, (v_constraint ->> 'targetValue')::numeric, 0);
                    v_ctol := greatest(0.05, coalesce((v_constraint ->> 'tolerance')::numeric, 0.5));
                    v_cweight := greatest(0, coalesce((v_constraint ->> 'score')::numeric, 0));
                exception when others then
                    continue;
                end;
                v_target_score := v_target_score + v_cweight;
                v_cok := false;

                begin
                    if v_ctype = 'distance' and jsonb_array_length(v_pts) >= 2 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        if v_ax is not null and v_ay is not null and v_bx is not null and v_by is not null then
                            v_dist1 := sqrt(power(v_bx - v_ax, 2) + power(v_by - v_ay, 2));
                            v_cok := v_dist1 >= 0.1 and abs(v_dist1 - v_cval) <= v_ctol + 0.000001;
                        end if;
                    elsif v_ctype = 'equal_length' and jsonb_array_length(v_pts) >= 4 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        v_cx := (v_pointset -> (v_pts ->> 2) ->> 'x')::numeric; v_cy := (v_pointset -> (v_pts ->> 2) ->> 'y')::numeric;
                        v_dx := (v_pointset -> (v_pts ->> 3) ->> 'x')::numeric; v_dy := (v_pointset -> (v_pts ->> 3) ->> 'y')::numeric;
                        if v_ax is not null and v_bx is not null and v_cx is not null and v_dx is not null then
                            v_dist1 := sqrt(power(v_bx - v_ax, 2) + power(v_by - v_ay, 2));
                            v_dist2 := sqrt(power(v_dx - v_cx, 2) + power(v_dy - v_cy, 2));
                            v_cok := v_dist1 >= 0.1 and abs(v_dist1 - v_dist2) <= v_ctol + 0.000001;
                        end if;
                    elsif v_ctype = 'midpoint' and jsonb_array_length(v_pts) >= 3 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        v_cx := (v_pointset -> (v_pts ->> 2) ->> 'x')::numeric; v_cy := (v_pointset -> (v_pts ->> 2) ->> 'y')::numeric;
                        if v_ax is not null and v_bx is not null and v_cx is not null then
                            v_dist1 := sqrt(power(v_cx - v_bx, 2) + power(v_cy - v_by, 2));
                            v_dist2 := sqrt(power(v_ax - (v_bx + v_cx) / 2.0, 2) + power(v_ay - (v_by + v_cy) / 2.0, 2));
                            v_cok := v_dist1 >= 0.1 and v_dist2 <= v_ctol + 0.000001;
                        end if;
                    elsif v_ctype = 'angle' and jsonb_array_length(v_pts) >= 3 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        v_cx := (v_pointset -> (v_pts ->> 2) ->> 'x')::numeric; v_cy := (v_pointset -> (v_pts ->> 2) ->> 'y')::numeric;
                        if v_ax is not null and v_bx is not null and v_cx is not null then
                            v_ux := v_ax - v_bx; v_uy := v_ay - v_by;
                            v_vx := v_cx - v_bx; v_vy := v_cy - v_by;
                            v_nu := sqrt(power(v_ux, 2) + power(v_uy, 2));
                            v_nv := sqrt(power(v_vx, 2) + power(v_vy, 2));
                            if v_nu > 0.000001 and v_nv > 0.000001 then
                                v_dot := greatest(-1, least(1, (v_ux * v_vx + v_uy * v_vy) / (v_nu * v_nv)));
                                v_deg := degrees(acos(v_dot::double precision))::numeric;
                                v_cok := abs(v_deg - v_cval) <= v_ctol + 0.000001;
                            end if;
                        end if;
                    elsif v_ctype = 'collinear' and jsonb_array_length(v_pts) >= 3 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        v_cx := (v_pointset -> (v_pts ->> 2) ->> 'x')::numeric; v_cy := (v_pointset -> (v_pts ->> 2) ->> 'y')::numeric;
                        if v_ax is not null and v_bx is not null and v_cx is not null then
                            v_dist1 := sqrt(power(v_cx - v_ax, 2) + power(v_cy - v_ay, 2));
                            if v_dist1 > 0.000001 then
                                v_dist2 := abs((v_cx - v_ax) * (v_by - v_ay) - (v_cy - v_ay) * (v_bx - v_ax)) / v_dist1;
                                v_cok := v_dist2 <= v_ctol + 0.000001;
                            end if;
                        end if;
                    elsif v_ctype in ('perpendicular', 'parallel') and jsonb_array_length(v_pts) >= 4 then
                        v_ax := (v_pointset -> (v_pts ->> 0) ->> 'x')::numeric; v_ay := (v_pointset -> (v_pts ->> 0) ->> 'y')::numeric;
                        v_bx := (v_pointset -> (v_pts ->> 1) ->> 'x')::numeric; v_by := (v_pointset -> (v_pts ->> 1) ->> 'y')::numeric;
                        v_cx := (v_pointset -> (v_pts ->> 2) ->> 'x')::numeric; v_cy := (v_pointset -> (v_pts ->> 2) ->> 'y')::numeric;
                        v_dx := (v_pointset -> (v_pts ->> 3) ->> 'x')::numeric; v_dy := (v_pointset -> (v_pts ->> 3) ->> 'y')::numeric;
                        if v_ax is not null and v_bx is not null and v_cx is not null and v_dx is not null then
                            v_ux := v_bx - v_ax; v_uy := v_by - v_ay;
                            v_vx := v_dx - v_cx; v_vy := v_dy - v_cy;
                            v_nu := sqrt(power(v_ux, 2) + power(v_uy, 2));
                            v_nv := sqrt(power(v_vx, 2) + power(v_vy, 2));
                            if v_nu > 0.000001 and v_nv > 0.000001 then
                                v_dot := least(1, abs((v_ux * v_vx + v_uy * v_vy) / (v_nu * v_nv)));
                                v_deg := degrees(acos(v_dot::double precision))::numeric;
                                if v_ctype = 'perpendicular' then
                                    v_cok := abs(90 - v_deg) <= v_ctol + 0.000001;
                                else
                                    v_cok := v_deg <= v_ctol + 0.000001;
                                end if;
                            end if;
                        end if;
                    end if;
                exception when others then
                    v_cok := false;
                end;

                if v_cok then
                    v_earned_score := v_earned_score + v_cweight;
                end if;
            end loop;
        end if;

        if v_target_score <= 0 then
            return 0;
        end if;
        return round(least(p_score, p_score * v_earned_score / v_target_score), 2);
    end if;

    if v_type = 'multiple' then
        if p_key ? 'correctOption' then
            begin
                return case when (v_resp_text)::numeric = (p_key ->> 'correctOption')::numeric then p_score else 0 end;
            exception when others then return 0; end;
        end if;
        return null;
    elsif v_type = 'truefalse' then
        if p_key ? 'correctAnswer' then
            return case when (lower(v_resp_text) in ('true', 't', '1')) = (lower(coalesce(p_key ->> 'correctAnswer', '')) in ('true', 't', '1')) then p_score else 0 end;
        end if;
        return null;
    elsif v_type = 'fill' then
        v_acc := coalesce(p_key -> 'accept', '[]'::jsonb);
        if jsonb_typeof(v_acc) <> 'array' or jsonb_array_length(v_acc) = 0 then return null; end if;
        v_cs := coalesce((p_key ->> 'caseSensitive')::boolean, false);
        for v_item in select value from jsonb_array_elements(v_acc) loop
            if v_cs then
                if trim(v_item #>> '{}') = trim(v_resp_text) then return p_score; end if;
            else
                if public.native_normalize_text_v1(v_item #>> '{}') = public.native_normalize_text_v1(v_resp_text) then return p_score; end if;
            end if;
        end loop;
        return 0;
    elsif v_type = 'numeric' then
        if not (p_key ? 'answer') or jsonb_typeof(p_key -> 'answer') = 'null' then return null; end if;
        begin
            v_ans := (p_key ->> 'answer')::numeric;
            v_tol := coalesce(nullif(p_key ->> 'tolerance', '')::numeric, 0);
            v_num := nullif(regexp_replace(public.native_normalize_text_v1(v_resp_text), '[^0-9.\-]', '', 'g'), '')::numeric;
        exception when others then
            return 0;
        end;
        if v_num is null then return 0; end if;
        return case when abs(v_num - v_ans) <= abs(v_tol) then p_score else 0 end;
    elsif v_type = 'matching' then
        if jsonb_typeof(coalesce(p_key -> 'matchAnswer', 'null'::jsonb)) <> 'object' then return null; end if;
        if jsonb_typeof(p_resp) <> 'object' then return 0; end if;
        for v_k in select key from jsonb_object_keys(p_key -> 'matchAnswer') as t(key) loop
            v_n := v_n + 1;
            begin
                if (p_resp ->> v_k) is not null and (p_resp ->> v_k)::numeric = (p_key -> 'matchAnswer' ->> v_k)::numeric then v_ok := v_ok + 1; end if;
            exception when others then null; end;
        end loop;
        if v_n = 0 then return null; end if;
        return round(p_score * v_ok / v_n, 2);
    end if;
    return null;
end;
$$;

revoke all on function public.native_auto_grade_question_v1(text, numeric, jsonb, jsonb) from public, anon;
grant execute on function public.native_auto_grade_question_v1(text, numeric, jsonb, jsonb) to authenticated;

-- ------------------------------------------------------------
-- ۴) submit_answer: اعمال قفل سرور بخش‌ها + رفع باگ آرایهٔ JSON + نمره‌دهی خودکار یکپارچه
-- ------------------------------------------------------------
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

      v_auto := public.native_auto_grade_question_v1(v_q ->> 'type', v_score, v_key_item, v_resp_el);
      if v_auto is not null then
        v_g := v_auto;
        if v_auto = 0
           and coalesce(v_exam.neg_marking, 0) > 0
           and lower(coalesce(v_q ->> 'type', '')) in ('multiple', 'truefalse')
           and v_resp_el is not null
           and jsonb_typeof(v_resp_el) <> 'null'
           and btrim(v_resp_el #>> '{}') <> '' then
          v_g := -1 * coalesce(v_exam.neg_marking, 0) * v_score;
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

revoke all on function public.submit_answer(text, jsonb, jsonb, jsonb) from public, anon;
grant execute on function public.submit_answer(text, jsonb, jsonb, jsonb) to authenticated;

-- ------------------------------------------------------------
-- ۵) کارنامهٔ دانش‌آموز و تأیید نمره‌های خودکار: بازگرداندن part_grades و حفظ محرمانگی شرط‌های هندسی
-- ------------------------------------------------------------
create or replace function public.native_my_answer_detail_parts_v1(p_answer text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
    v_part_grades jsonb;
    v_feedback jsonb;
    v_safe_questions jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_result := public.native_my_answer_detail_v1(p_answer);
    if v_result ? 'error' then return v_result; end if;
    select coalesce(a.part_grades, '{}'::jsonb), coalesce(a.part_feedback, '{}'::jsonb)
      into v_part_grades, v_feedback
      from public.answers a
     where a.id::text = btrim(coalesce(p_answer, '')) and a.student_id = v_uid;
    if not found then return jsonb_build_object('error', 'پاسخ پیدا نشد یا متعلق به این حساب نیست'); end if;
    select coalesce(jsonb_agg(q.item - 'rubric' - 'partRubrics' - 'constructionTargets' - 'constructionConstraints'), '[]'::jsonb)
      into v_safe_questions
      from jsonb_array_elements(coalesce(v_result -> 'questions', '[]'::jsonb)) as q(item);
    return v_result || jsonb_build_object(
      'part_grades', v_part_grades,
      'part_feedback', v_feedback,
      'questions', v_safe_questions
    );
end;
$$;

revoke all on function public.native_my_answer_detail_parts_v1(text) from public, anon;
grant execute on function public.native_my_answer_detail_parts_v1(text) to authenticated;

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
            if lower(coalesce(v_q ->> 'type', '')) in ('essay', 'long', 'descriptive') and not v_auto_construct then
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

revoke all on function public.exam_autograde_info(text) from public, anon;
grant execute on function public.exam_autograde_info(text) to authenticated;

create or replace function public.approve_auto_grades(p_exam text, p_mode text default 'auto_only')
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb;
    v_q jsonb;
    v_key jsonb;
    v_n integer;
    v_i integer;
    v_has_essay boolean := false;
    v_auto_construct boolean;
    v_ans record;
    v_grades jsonb;
    v_old jsonb;
    v_g numeric;
    v_total numeric;
    v_count integer := 0;
    v_prefilled integer := 0;
    v_resp jsonb;
    v_objective integer[] := '{}';
    v_essay_q integer[] := '{}';
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;

    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid limit 1;
    if v_exam.id is null then return jsonb_build_object('error', 'آزمون پیدا نشد یا متعلق به این حساب نیست'); end if;
    if jsonb_typeof(coalesce(v_exam.questions, 'null'::jsonb)) <> 'array' then
        return jsonb_build_object('error', 'ساختار سؤال‌های آزمون نامعتبر است');
    end if;
    v_n := jsonb_array_length(v_exam.questions);

    select coalesce(k.answers, '[]'::jsonb) into v_keys from public.exam_keys k where k.exam_id = v_exam.id limit 1;
    v_keys := coalesce(v_keys, '[]'::jsonb);
    if jsonb_typeof(v_keys) <> 'array' then v_keys := '[]'::jsonb; end if;

    for v_i in 0 .. v_n - 1 loop
        select coalesce(item, '{}'::jsonb) into v_key
        from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
        where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
        order by ord limit 1;
        v_key := coalesce(v_key, '{}'::jsonb);
        v_auto_construct := (jsonb_typeof(v_key -> 'constructionTargets') = 'array' and jsonb_array_length(v_key -> 'constructionTargets') > 0)
                         or (jsonb_typeof(v_key -> 'constructionConstraints') = 'array' and jsonb_array_length(v_key -> 'constructionConstraints') > 0);
        if lower(coalesce(v_exam.questions -> v_i ->> 'type', '')) in ('essay', 'long', 'descriptive') and not v_auto_construct then
            v_has_essay := true;
            v_essay_q := v_essay_q || (v_i + 1);
        end if;
    end loop;

    for v_ans in
        select a.id, a.responses, a.grades, a.graded, a.auto_graded
        from public.answers a
        where a.exam_id = v_exam.id
          and (coalesce(a.graded, false) = false or coalesce(a.auto_graded, false) = true)
    loop
        v_grades := '[]'::jsonb;
        v_total := 0;
        v_objective := '{}';
        v_old := case when jsonb_typeof(coalesce(v_ans.grades, 'null'::jsonb)) = 'array' then v_ans.grades else '[]'::jsonb end;
        for v_i in 0 .. v_n - 1 loop
            v_q := coalesce(v_exam.questions -> v_i, '{}'::jsonb);
            select coalesce(item, '{}'::jsonb) into v_key
            from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
            where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
            order by ord limit 1;
            v_key := coalesce(v_key, '{}'::jsonb);
            v_resp := case when jsonb_typeof(coalesce(v_ans.responses, 'null'::jsonb)) = 'array' then v_ans.responses -> v_i else null end;
            v_g := public.native_auto_grade_question_v1(v_q ->> 'type', coalesce(nullif(v_q ->> 'score', '')::numeric, 0), v_key, v_resp);
            if v_g is null then
                begin v_g := coalesce(nullif(v_old ->> v_i, '')::numeric, 0); exception when others then v_g := 0; end;
            else
                v_objective := v_objective || v_i;
            end if;
            v_grades := v_grades || to_jsonb(v_g);
            v_total := v_total + v_g;
        end loop;

        if v_has_essay and coalesce(p_mode, 'auto_only') = 'auto_only' then
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = false, auto_graded = true, graded_at = null
            where id = v_ans.id and exam_id = v_exam.id
              and (coalesce(graded, false) = false or coalesce(auto_graded, false) = true);
            if found then
                insert into public.native_grade_progress(answer_id, question_index, teacher_id, updated_at)
                select v_ans.id, q, v_uid, now() from unnest(v_objective) as q
                on conflict (answer_id, question_index) do update set updated_at = excluded.updated_at;
                v_prefilled := v_prefilled + 1;
            end if;
        else
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = true, auto_graded = true, graded_at = now()::text
            where id = v_ans.id and exam_id = v_exam.id;
            v_count := v_count + 1;
        end if;
    end loop;

    return jsonb_build_object('ok', true, 'count', v_count, 'prefilled', v_prefilled, 'has_essay', v_has_essay,
        'essay_questions', to_jsonb(v_essay_q));
end;
$$;

revoke all on function public.approve_auto_grades(text, text) from public, anon;
grant execute on function public.approve_auto_grades(text, text) to authenticated;

commit;
notify pgrst, 'reload schema';
