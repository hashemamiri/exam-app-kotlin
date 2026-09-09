-- V135 بخش ۲/۲ — سیاست‌های storage برای پوشهٔ audio (بعد از بخش ۱ اجرا شود).
-- lock_timeout: اگر سرویس Storage جدول را قفل کرده بود به‌جای بن‌بست خطا می‌دهد؛ چند ثانیه بعد دوباره Run بزنید.
begin;
set local lock_timeout = '8s';
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

commit;
