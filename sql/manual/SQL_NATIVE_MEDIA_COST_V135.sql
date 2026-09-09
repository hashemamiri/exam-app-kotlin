-- V135 — هزینهٔ رسانهٔ سؤال در آزمون آنلاین (درخواست کاربر):
--   هر سؤالِ مشمول (جدید/تغییرکرده — همان قاعدهٔ قبلی) = ۱۰۰۰ تومان
--   + هر تصویر همان سؤال (تصاویر سؤال + گزینه‌ها + جورکردنی) = ۱۰۰۰ تومان
--   + فایل صوتی همان سؤال: تا ۱MB = ۲۰۰۰، تا ۲MB = ۴۰۰۰، تا ۳MB = ۶۰۰۰ تومان (بیشتر از ۳MB رد می‌شود)
-- فیلدهای جدید سؤال: audio (URL)، audioBytes (حجم پس از فشرده‌سازی)، audioMs (مدت).
-- خروجی native_save_exam_v1 تفکیک هزینه را برمی‌گرداند تا پنجرهٔ کسر هزینه شفاف باشد.
-- native_save_exam_v2 (V61) بدون تغییر همین v1 را صدا می‌زند.

begin;

create or replace function public.native_question_media_cost_v135(p_q jsonb)
returns jsonb
language plpgsql
immutable
set search_path = public, pg_temp
as $$
declare
    v_images integer := 0;
    v_audio_bytes bigint := 0;
    v_audio_cost bigint := 0;
    v_arr jsonb;
begin
    if p_q is null or jsonb_typeof(p_q) <> 'object' then
        return jsonb_build_object('images', 0, 'image_cost', 0, 'audio_cost', 0);
    end if;
    -- تصاویر خودِ سؤال (images یا image قدیمی)
    v_arr := p_q->'images';
    if jsonb_typeof(v_arr) = 'array' then
        select count(*) into v_images from jsonb_array_elements_text(v_arr) t where btrim(coalesce(t,'')) <> '';
    elsif btrim(coalesce(p_q->>'image','')) <> '' then
        v_images := 1;
    end if;
    -- تصاویر گزینه‌ها و جورکردنی
    foreach v_arr in array array[p_q->'optionImages', p_q->'leftImages', p_q->'rightImages'] loop
        if v_arr is not null and jsonb_typeof(v_arr) = 'array' then
            v_images := v_images + (select count(*) from jsonb_array_elements_text(v_arr) t where btrim(coalesce(t,'')) <> '');
        end if;
    end loop;
    -- صوت
    if btrim(coalesce(p_q->>'audio','')) <> '' then
        v_audio_bytes := greatest(1, coalesce((p_q->>'audioBytes')::bigint, 1));
        if v_audio_bytes > 3 * 1024 * 1024 then
            raise exception 'حجم فایل صوتی یکی از سؤال‌ها بیش از ۳ مگابایت است';
        end if;
        v_audio_cost := case
            when v_audio_bytes <= 1 * 1024 * 1024 then 2000
            when v_audio_bytes <= 2 * 1024 * 1024 then 4000
            else 6000 end;
    end if;
    return jsonb_build_object('images', v_images, 'image_cost', v_images * 1000, 'audio_cost', v_audio_cost);
exception when invalid_text_representation then
    raise exception 'حجم فایل صوتی نامعتبر است';
end;
$$;
revoke all on function public.native_question_media_cost_v135(jsonb) from public, anon, authenticated;

create or replace function public.native_save_exam_v1(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_operation uuid;
    v_exam_id text;
    v_code text;
    v_title text;
    v_subject text;
    v_questions jsonb;
    v_keys jsonb;
    v_mode text;
    v_old public.exams%rowtype;
    v_old_keys jsonb := '[]'::jsonb;
    v_create boolean;
    v_has_answers boolean := false;
    v_billable integer := 0;
    v_cost bigint := 0;
    -- V135 — تفکیک هزینه: سؤال / تصویر / صوت
    v_img_count integer := 0;
    v_img_cost bigint := 0;
    v_audio_count integer := 0;
    v_audio_cost bigint := 0;
    v_billed_keys text[] := '{}';
    v_balance bigint := 0;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم دسترسی دارد');
    end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
        return jsonb_build_object('error', 'داده آزمون نامعتبر است');
    end if;
    if coalesce(p_payload->>'operation_id', '') !~* '^[0-9a-f-]{36}$' then
        return jsonb_build_object('error', 'شناسه عملیات نامعتبر است');
    end if;
    v_operation := (p_payload->>'operation_id')::uuid;

    select result into v_prior
    from public.native_exam_operations
    where operation_id = v_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = v_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    v_exam_id := btrim(coalesce(p_payload->>'id', ''));
    v_title := btrim(coalesce(p_payload->>'title', ''));
    v_subject := btrim(coalesce(p_payload->>'subject', ''));
    v_questions := p_payload->'questions';
    v_keys := p_payload->'answer_key';
    v_mode := coalesce(p_payload->>'audience', 'all');
    if v_exam_id = '' or length(v_exam_id) > 100 then return jsonb_build_object('error', 'شناسه آزمون نامعتبر است'); end if;
    if v_title = '' or length(v_title) > 250 then return jsonb_build_object('error', 'عنوان آزمون نامعتبر است'); end if;
    if length(v_subject) > 250 then return jsonb_build_object('error', 'نام درس بیش از حد بلند است'); end if;
    if jsonb_typeof(v_questions) <> 'array' or jsonb_array_length(v_questions) < 1 or jsonb_array_length(v_questions) > 500 then
        return jsonb_build_object('error', 'تعداد سؤال‌ها باید بین ۱ و ۵۰۰ باشد');
    end if;
    if jsonb_typeof(v_keys) <> 'array' then return jsonb_build_object('error', 'کلید پاسخ نامعتبر است'); end if;
    if v_mode not in ('all', 'classes', 'students') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;
    if v_mode = 'classes' and coalesce(jsonb_array_length(p_payload->'classes'), 0) = 0 then
        return jsonb_build_object('error', 'حداقل یک کلاس انتخاب کنید');
    end if;
    if v_mode = 'students' and coalesce(jsonb_array_length(p_payload->'students'), 0) = 0 then
        return jsonb_build_object('error', 'حداقل یک دانش‌آموز انتخاب کنید');
    end if;

    select * into v_old from public.exams where id = v_exam_id;
    v_create := not found;
    if not v_create and v_old.teacher_id <> v_uid then
        return jsonb_build_object('error', 'آزمون متعلق به این حساب نیست');
    end if;

    if v_create then
        v_code := upper(btrim(coalesce(p_payload->>'code', '')));
        if v_code !~ '^[A-Z0-9]{4,12}$' then return jsonb_build_object('error', 'کد آزمون نامعتبر است'); end if;
        if exists (select 1 from public.exams where upper(code) = v_code) then
            return jsonb_build_object('error', 'کد آزمون تکراری است؛ دوباره تلاش کنید');
        end if;
        select coalesce(array_agg(n.question_key), '{}') into v_billed_keys
        from public.native_question_fingerprints(v_questions, v_keys) n;
    else
        v_code := v_old.code;
        select coalesce(answers, '[]'::jsonb) into v_old_keys
        from public.exam_keys where exam_id = v_exam_id;
        v_has_answers := exists (select 1 from public.answers where exam_id = v_exam_id);
        if v_has_answers then
            select coalesce(array_agg(n.question_key), '{}') into v_billed_keys
            from public.native_question_fingerprints(v_questions, v_keys) n
            left join public.native_question_fingerprints(v_old.questions, v_old_keys) o using (question_key)
            where o.question_key is null or n.fingerprint is distinct from o.fingerprint;
        else
            select coalesce(array_agg(n.question_key), '{}') into v_billed_keys
            from public.native_question_fingerprints(v_questions, v_keys) n
            left join public.native_question_fingerprints(v_old.questions, v_old_keys) o using (question_key)
            where o.question_key is null;
        end if;
    end if;
    v_billable := coalesce(array_length(v_billed_keys, 1), 0);
    -- V135 — هزینهٔ رسانه فقط برای همان سؤال‌های مشمول (جدید/تغییرکرده):
    -- هر تصویر (سؤال + گزینه‌ها + جورکردنی) ۱۰۰۰ تومان؛ صوت تا ۱MB ۲۰۰۰، تا ۲MB ۴۰۰۰، تا ۳MB ۶۰۰۰.
    select coalesce(sum((m->>'images')::integer), 0),
           coalesce(sum((m->>'image_cost')::bigint), 0),
           coalesce(sum(case when (m->>'audio_cost')::bigint > 0 then 1 else 0 end), 0),
           coalesce(sum((m->>'audio_cost')::bigint), 0)
      into v_img_count, v_img_cost, v_audio_count, v_audio_cost
    from jsonb_array_elements(v_questions) with ordinality q(item, ord)
    cross join lateral public.native_question_media_cost_v135(q.item) m
    where coalesce(nullif(q.item->>'id', ''), '@' || q.ord::text) = any(v_billed_keys);
    v_cost := v_billable * 1000 + v_img_cost + v_audio_cost;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object(
            'error', 'موجودی کیف پول کافی نیست',
            'balance', v_balance,
            'required', v_cost
        );
    end if;

    if v_create then
        insert into public.exams(
            id, teacher_id, title, subject, duration, code, total_score, is_open,
            shuffle_q, shuffle_opt, neg_marking, audience, teacher_message,
            attempts_allowed, attempt_on_timeout, grade_policy, attempt_cooldown, questions
        ) values (
            v_exam_id, v_uid, v_title, v_subject,
            greatest(0, least(1440, coalesce((p_payload->>'duration')::integer, 0))),
            v_code,
            greatest(0, coalesce((p_payload->>'total_score')::double precision, 0)),
            false,
            coalesce((p_payload->>'shuffle_q')::boolean, false),
            coalesce((p_payload->>'shuffle_opt')::boolean, false),
            greatest(0, coalesce((p_payload->>'neg_marking')::numeric, 0)),
            v_mode,
            nullif(btrim(coalesce(p_payload->>'teacher_message', '')), ''),
            greatest(1, least(5, coalesce((p_payload->>'attempts_allowed')::integer, 1))),
            coalesce((p_payload->>'attempt_on_timeout')::boolean, false),
            case when p_payload->>'grade_policy' in ('last', 'best', 'all') then p_payload->>'grade_policy' else 'last' end,
            greatest(0, least(1440, coalesce((p_payload->>'attempt_cooldown')::integer, 0))),
            v_questions
        );
    else
        update public.exams
        set title = v_title,
            subject = v_subject,
            duration = greatest(0, least(1440, coalesce((p_payload->>'duration')::integer, 0))),
            total_score = greatest(0, coalesce((p_payload->>'total_score')::double precision, 0)),
            shuffle_q = coalesce((p_payload->>'shuffle_q')::boolean, false),
            shuffle_opt = coalesce((p_payload->>'shuffle_opt')::boolean, false),
            neg_marking = greatest(0, coalesce((p_payload->>'neg_marking')::numeric, 0)),
            audience = v_mode,
            teacher_message = nullif(btrim(coalesce(p_payload->>'teacher_message', '')), ''),
            attempts_allowed = greatest(1, least(5, coalesce((p_payload->>'attempts_allowed')::integer, 1))),
            attempt_on_timeout = coalesce((p_payload->>'attempt_on_timeout')::boolean, false),
            grade_policy = case when p_payload->>'grade_policy' in ('last', 'best', 'all') then p_payload->>'grade_policy' else 'last' end,
            attempt_cooldown = greatest(0, least(1440, coalesce((p_payload->>'attempt_cooldown')::integer, 0))),
            questions = v_questions
        where id = v_exam_id and teacher_id = v_uid;
    end if;

    insert into public.exam_keys(exam_id, answers)
    values (v_exam_id, v_keys)
    on conflict (exam_id) do update set answers = excluded.answers;

    delete from public.exam_audience_classes where exam_id = v_exam_id;
    delete from public.exam_audience_students where exam_id = v_exam_id;
    if v_mode = 'classes' then
        insert into public.exam_audience_classes(exam_id, class_id)
        select v_exam_id, c.id
        from public.classes c
        join (select distinct value::uuid as id from jsonb_array_elements_text(p_payload->'classes')) wanted on wanted.id = c.id
        where c.teacher_id = v_uid;
        if (select count(*) from public.exam_audience_classes where exam_id = v_exam_id) <>
           (select count(distinct value) from jsonb_array_elements_text(p_payload->'classes')) then
            raise exception 'کلاس مخاطب نامعتبر است';
        end if;
    elsif v_mode = 'students' then
        insert into public.exam_audience_students(exam_id, student_id)
        select v_exam_id, p.id
        from public.profiles p
        join (select distinct value::uuid as id from jsonb_array_elements_text(p_payload->'students')) wanted on wanted.id = p.id
        where p.teacher_id = v_uid and p.role = 'student';
        if (select count(*) from public.exam_audience_students where exam_id = v_exam_id) <>
           (select count(distinct value) from jsonb_array_elements_text(p_payload->'students')) then
            raise exception 'دانش‌آموز مخاطب نامعتبر است';
        end if;
    end if;

    if v_cost > 0 then
        update public.wallets
        set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid
        returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (
            v_uid,
            -v_cost,
            case when v_create then 'exam:create:' else 'exam:update:' end || v_exam_id,
            v_balance,
            v_operation
        );
    end if;

    v_result := jsonb_build_object(
        'ok', true,
        'id', v_exam_id,
        'code', v_code,
        'billed_questions', v_billable,
        'billed_images', v_img_count,
        'image_cost', v_img_cost,
        'billed_audio', v_audio_count,
        'audio_cost', v_audio_cost,
        'question_cost', v_billable * 1000,
        'cost', v_cost,
        'balance', v_balance
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (v_operation, v_uid, v_exam_id, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_save_exam_v1(jsonb) from public, anon;
grant execute on function public.native_save_exam_v1(jsonb) to authenticated;

-- سیاست آپلود باکت: پوشهٔ audio برای فایل صوتی سؤال (مسیر audio/<teacher>/<exam>/…)
drop policy if exists v11_authenticated_upload_exam_images on storage.objects;
create policy v11_authenticated_upload_exam_images
on storage.objects for insert to authenticated
with check (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] in ('avatars','questions','option_images','matching','answers','audio')
    and (storage.foldername(name))[2] = auth.uid()::text
);
drop policy if exists v75_8_read_question_images on storage.objects;
create policy v75_8_read_question_images
on storage.objects for select to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] in ('questions', 'option_images', 'matching', 'audio')
);

notify pgrst, 'reload schema';
commit;
