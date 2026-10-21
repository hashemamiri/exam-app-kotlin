-- Persist independent scores and feedback for structured essay parts.
begin;

alter table public.answers
    add column if not exists part_grades jsonb not null default '{}'::jsonb,
    add column if not exists part_feedback jsonb not null default '{}'::jsonb;

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

    select a.exam_id, e.questions into v_exam_id, v_questions
    from public.answers a join public.exams e on e.id = a.exam_id
    where a.id = p_answer and e.teacher_id = v_uid limit 1;
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
        begin
            v_value := (p_part_grades ->> v_part_key)::numeric;
            v_max := coalesce((v_questions -> v_q -> 'assessmentParts' -> v_p ->> 'score')::numeric, 0);
        exception when others then
            return jsonb_build_object('error', 'نمرهٔ زیربخش نامعتبر است');
        end;
        if v_value < 0 or v_value > v_max then
            return jsonb_build_object('error', format('نمرهٔ زیربخش %s خارج از بازه است', v_p + 1));
        end if;
    end loop;

    for v_index in 0..jsonb_array_length(coalesce(v_questions, '[]'::jsonb)) - 1 loop
        v_part_count := case when jsonb_typeof(v_questions -> v_index -> 'assessmentParts') = 'array'
            then jsonb_array_length(v_questions -> v_index -> 'assessmentParts') else 0 end;
        if v_part_count > 0 then
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
                - 'tolerance' - 'matchAnswer' - 'explanation' - 'rubric' - 'partRubrics' - 'constructionTargets';
            v_key := '{}'::jsonb;
            if coalesce(v_answer.graded, false) then
                select coalesce(item, '{}'::jsonb) into v_key
                from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
                where coalesce((item->>'i')::integer, (ord - 1)::integer) = v_index
                order by ord
                limit 1;
                v_key := coalesce(v_key, '{}'::jsonb) - 'i' - 'rubric' - 'partRubrics' - 'constructionTargets';
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
    v_feedback jsonb;
    v_safe_questions jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_result := public.native_my_answer_detail_v1(p_answer);
    if v_result ? 'error' then return v_result; end if;
    select coalesce(a.part_feedback, '{}'::jsonb) into v_feedback
    from public.answers a where a.id::text = btrim(coalesce(p_answer, '')) and a.student_id = v_uid;
    if not found then return jsonb_build_object('error', 'پاسخ پیدا نشد یا متعلق به این حساب نیست'); end if;
    select coalesce(jsonb_agg(q.item - 'rubric' - 'partRubrics' - 'constructionTargets'), '[]'::jsonb)
      into v_safe_questions
      from jsonb_array_elements(coalesce(v_result -> 'questions', '[]'::jsonb)) as q(item);
    return v_result || jsonb_build_object('part_feedback', v_feedback, 'questions', v_safe_questions);
end;
$$;

revoke all on function public.native_my_answer_detail_parts_v1(text) from public, anon;
grant execute on function public.native_my_answer_detail_parts_v1(text) to authenticated;

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
    v_pointset jsonb;
    v_target jsonb;
    v_actual jsonb;
    v_label text;
    v_target_x numeric;
    v_target_y numeric;
    v_actual_x numeric;
    v_actual_y numeric;
    v_point_tol numeric;
    v_target_score numeric := 0;
    v_earned_score numeric := 0;
begin
    if v_type in ('essay', 'long', 'descriptive') and not (coalesce(p_key, '{}'::jsonb) ? 'constructionTargets') then return null; end if;
    if p_resp is null or jsonb_typeof(p_resp) = 'null' or (jsonb_typeof(p_resp) = 'string' and trim(p_resp #>> '{}') = '') then
        return 0;
    end if;
    v_resp_text := p_resp #>> '{}';

    if v_type in ('essay', 'long', 'descriptive') and p_key ? 'constructionTargets' then
        v_targets := coalesce(p_key -> 'constructionTargets', '[]'::jsonb);
        if jsonb_typeof(v_targets) <> 'array' or jsonb_array_length(v_targets) = 0 then return 0; end if;
        if (select count(distinct t.item ->> 'label') from jsonb_array_elements(v_targets) as t(item)) <> jsonb_array_length(v_targets)
           or exists (select 1 from jsonb_array_elements(v_targets) as t(item) where jsonb_typeof(t.item) <> 'object' or nullif(t.item ->> 'label', '') is null) then return 0; end if;
        begin v_pointset := v_resp_text::jsonb; exception when others then return 0; end;
        if jsonb_typeof(v_pointset) <> 'object' then return 0; end if;
        for v_target in select value from jsonb_array_elements(v_targets) loop
            v_label := v_target ->> 'label';
            begin
                v_target_x := (v_target ->> 'x')::numeric;
                v_target_y := (v_target ->> 'y')::numeric;
                v_point_tol := greatest(0, (v_target ->> 'tolerance')::numeric);
                v_target_score := v_target_score + greatest(0, (v_target ->> 'score')::numeric);
            exception when others then return 0; end;
            if v_target_x < -5 or v_target_x > 5 or v_target_y < -5 or v_target_y > 5 or v_point_tol <= 0 or v_point_tol > 5 then return 0; end if;
            v_actual := v_pointset -> v_label;
            if jsonb_typeof(v_actual) = 'object' then
                begin
                    v_actual_x := (v_actual ->> 'x')::numeric;
                    v_actual_y := (v_actual ->> 'y')::numeric;
                    if sqrt(power(v_actual_x - v_target_x, 2) + power(v_actual_y - v_target_y, 2)) <= v_point_tol then
                        v_earned_score := v_earned_score + greatest(0, (v_target ->> 'score')::numeric);
                    end if;
                exception when others then null; end;
            end if;
        end loop;
        if v_target_score <= 0 then return 0; end if;
        return round(p_score * v_earned_score / v_target_score, 2);
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

revoke all on function public.native_save_grade_parts_v1(text, jsonb, text, jsonb, jsonb) from public, anon;
grant execute on function public.native_save_grade_parts_v1(text, jsonb, text, jsonb, jsonb) to authenticated;

commit;
notify pgrst, 'reload schema';
