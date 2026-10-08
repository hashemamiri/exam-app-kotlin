-- V221 — همهٔ آدرس‌های رسانهٔ یک آزمون (باکت سوپابیس + فضای ابری S3/R2) برای پاک‌سازی هنگام حذف آزمون.
-- جایگزین native_exam_image_paths_v59 که فقط آدرس‌های exam-images را برمی‌گرداند. اجرای دستی توسط مالک پروژه.
create or replace function public.native_exam_media_urls_v221(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_questions jsonb;
    v_urls jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select questions into v_questions from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;

    select coalesce(jsonb_agg(distinct value), '[]'::jsonb) into v_urls
    from jsonb_path_query(coalesce(v_questions, '[]'::jsonb), 'lax $.**') as t(value)
    where jsonb_typeof(value) = 'string'
      and (
        value #>> '{}' like '%/storage/v1/object/public/exam-images/%'
        or value #>> '{}' ~ '^https://[^\s"'']+/(questions|option_images|matching_images|audio)/[0-9a-fA-F-]{36}/'
      );

    return jsonb_build_object('ok', true, 'urls', v_urls);
end;
$$;
revoke all on function public.native_exam_media_urls_v221(text) from public, anon;
grant execute on function public.native_exam_media_urls_v221(text) to authenticated;
