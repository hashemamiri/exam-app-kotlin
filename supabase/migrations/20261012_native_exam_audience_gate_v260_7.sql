-- V260.7 — get_exam_for_student: دروازهٔ مخاطبان (کلاس‌ها / دانش‌آموزان خاص / مدرسه‌ها).
-- یافتهٔ شبیه‌سازی: آزمون با مخاطب «۴ دانش‌آموز خاص» را هر ۳۰ دانش‌آموز با کد توانستند بدهند.
-- بدنهٔ تابع عیناً از V255 است؛ فقط بلوک «دروازهٔ مخاطبان» بعد از بررسی فهرست معلم اضافه شده.
-- اجرای دستی در SQL Editor سوپابیس.

begin;

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

commit;
