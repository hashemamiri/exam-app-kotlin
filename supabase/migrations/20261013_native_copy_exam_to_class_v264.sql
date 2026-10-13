-- V264 — کپی یک‌کلیکی آزمون به کلاس دیگر (آیتم ۱۱ فهرست بهبود).
-- مثل native_duplicate_exam_v2 (idempotent با p_operation، کسر هزینه از کیف پول) ولی:
--   * همهٔ ستون‌های آزمون مبدأ کپی می‌شوند (jsonb_populate_record؛ ستون‌های جدید هم جا نمی‌مانند)
--   * مخاطب = 'classes' و کلاس مقصد در exam_audience_classes ثبت می‌شود
--   * عنوان = «عنوان مبدأ — نام کلاس»
--   * هزینه: هر سؤال ۱۰۰۰ تومان + تصویر/صوت طبق native_question_media_cost_v135 (مثل ذخیرهٔ آزمون)
create or replace function public.native_duplicate_exam_to_class_v1(p_exam text, p_class uuid, p_operation uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_source public.exams%rowtype;
    v_new public.exams%rowtype;
    v_class_name text;
    v_new_id text := gen_random_uuid()::text;
    v_code text;
    v_try integer := 0;
    v_count integer;
    v_media bigint := 0;
    v_cost bigint;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    if p_class is null then return jsonb_build_object('error', 'کلاس مقصد را انتخاب کنید'); end if;
    select result into v_prior from public.native_exam_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    select * into v_source from public.exams where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    select c.name into v_class_name from public.classes c where c.id = p_class and c.teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'کلاس مقصد پیدا نشد یا متعلق به شما نیست'); end if;

    v_count := jsonb_array_length(coalesce(v_source.questions, '[]'::jsonb));
    select coalesce(sum((m->>'image_cost')::bigint + (m->>'audio_cost')::bigint), 0) into v_media
    from jsonb_array_elements(coalesce(v_source.questions, '[]'::jsonb)) q(item)
    cross join lateral public.native_question_media_cost_v135(q.item) m;
    v_cost := v_count * 1000 + v_media;

    insert into public.wallets(user_id, balance) values (v_uid, 0) on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;

    loop
        v_try := v_try + 1;
        v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
        exit when not exists (select 1 from public.exams where code = v_code);
        if v_try >= 20 then raise exception 'ساخت کد یکتا ناموفق بود'; end if;
    end loop;

    v_new := jsonb_populate_record(v_source, jsonb_build_object(
        'id', v_new_id,
        'title', coalesce(v_source.title, '') || ' — ' || coalesce(v_class_name, ''),
        'code', v_code,
        'is_open', false,
        'opens_at', null,
        'closes_at', null,
        'class_id', null,
        'audience', 'classes',
        'teacher_id', v_uid,
        'created_at', now()
    ));
    insert into public.exams select (v_new).*;
    insert into public.exam_keys(exam_id, answers)
    select v_new_id, answers from public.exam_keys where exam_id = p_exam;
    insert into public.exam_audience_classes(exam_id, class_id) values (v_new_id, p_class);

    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'exam:copy_to_class:' || v_new_id, v_balance, p_operation);
    end if;

    v_result := jsonb_build_object(
        'ok', true, 'id', v_new_id, 'code', v_code, 'class_name', v_class_name,
        'billed_questions', v_count, 'media_cost', v_media, 'cost', v_cost, 'balance', v_balance
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, v_new_id, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_duplicate_exam_to_class_v1(text,uuid,uuid) from public, anon;
grant execute on function public.native_duplicate_exam_to_class_v1(text,uuid,uuid) to authenticated;
