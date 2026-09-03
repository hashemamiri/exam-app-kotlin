-- V75.8 — خصوصی‌سازی باکت تصاویر (بند ۲.۱ گزارش امنیتی).
-- ریشه: باکت exam-images عمومی بود و سیاست v11_public_read_exam_images خواندنِ
-- همه چیز را برای «public» (یعنی هر کسی، حتی بدون ورود) آزاد می‌کرد؛ تصاویر
-- پاسخِ دانش‌آموز (دست‌خط/برگه) و آواتارها هم با داشتنِ نشانی قابل مشاهده بودند.
--
-- از این به بعد:
--   - باکت خصوصی است (public = false)
--   - خواندن فقط برای کاربرِ «واردشده» و فقط طبق نقش/مالکیت مجاز است
--   - برنامه برای نمایش تصویر، توکنِ نشست را می‌فرستد (SupabaseAuthImageInterceptor)
--
-- نکتهٔ عملیاتی: نشانی‌های ذخیره‌شده در دیتابیس تغییر نمی‌کنند؛ فقط دسترسی
-- به آن‌ها از این پس نیازمندِ احراز هویت است. توابع آپلود/حذف دست‌نخورده می‌مانند.

begin;

update storage.buckets
set public = false
where id = 'exam-images';

drop policy if exists v11_public_read_exam_images on storage.objects;

-- ۱) تصاویر سؤال و گزینه‌ها: هر کاربرِ واردشده (دانش‌آموزِ آزمون یا کادر مدرسه)
create policy v75_8_read_question_images
on storage.objects for select to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] in ('questions', 'option_images', 'matching')
);

-- ۲) آواتارها: فقط کاربرانِ واردشده (نمای عمومیِ حساب، نه اینترنتِ باز)
create policy v75_8_read_avatars
on storage.objects for select to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] = 'avatars'
);

-- ۳) پاسخ‌های تصویریِ دانش‌آموز: فقط خودِ آن دانش‌آموز
create policy v75_8_student_read_own_answers
on storage.objects for select to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] = 'answers'
    and (storage.foldername(name))[2] = auth.uid()::text
);

-- ۴) پاسخ‌های تصویری: معلمِ صاحبِ همان آزمون (مسیر: answers/<student>/<exam>/<question>)
create policy v75_8_teacher_read_exam_answers
on storage.objects for select to authenticated
using (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] = 'answers'
    and exists (
        select 1
        from public.exams e
        where e.id::text = (storage.foldername(name))[3]
          and e.teacher_id = auth.uid()
    )
);

commit;

-- بررسیِ نتیجه:
-- select id, public from storage.buckets where id = 'exam-images';
-- select policyname, roles, cmd from pg_policies
-- where schemaname = 'storage' and tablename = 'objects' and policyname like 'v75_8%';
