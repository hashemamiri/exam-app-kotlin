-- V59.3 — حذف تصاویر استوریج همراه حذف سؤال/آزمون/عکس پروفایل.
-- الف) policy حذف: هر کاربر فقط فایل‌های پوشهٔ خودش را می‌تواند پاک کند
--     (همان قرارداد آپلود: foldername[2] = uid).
drop policy if exists v59_owner_delete_exam_images on storage.objects;
create policy v59_owner_delete_exam_images
on storage.objects for delete to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] in ('avatars','questions','option_images','matching','answers')
    and (storage.foldername(name))[2] = auth.uid()::text
);

-- ب) فهرست مسیرهای تصاویر یک آزمونِ خود معلم (برای پاک‌سازی پس از حذف).
create or replace function public.native_exam_image_paths_v59(p_exam text)
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

    -- همهٔ رشته‌های URL استوریج داخل JSON سؤال‌ها
    select coalesce(jsonb_agg(distinct value), '[]'::jsonb) into v_urls
    from jsonb_path_query(coalesce(v_questions, '[]'::jsonb), 'lax $.**') as t(value)
    where jsonb_typeof(value) = 'string'
      and value #>> '{}' like '%/storage/v1/object/public/exam-images/%';

    return jsonb_build_object('ok', true, 'urls', v_urls);
end;
$$;
revoke all on function public.native_exam_image_paths_v59(text) from public, anon;
grant execute on function public.native_exam_image_paths_v59(text) to authenticated;
