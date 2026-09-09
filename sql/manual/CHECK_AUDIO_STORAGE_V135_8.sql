-- V135.8 — بررسی سریع دلیل «دانلود فایل صوتی ممکن نشد». هر سه پرس‌وجو را اجرا کنید:
-- ۱) سیاست خواندن باید پوشهٔ audio را داشته باشد (ستون qual باید شامل 'audio' باشد)
select policyname, cmd, qual
from pg_policies
where schemaname = 'storage' and tablename = 'objects'
  and policyname in ('v75_8_read_question_images', 'v11_authenticated_upload_exam_images');

-- ۲) فایل‌های صوتی آپلودشده و نوع MIME آن‌ها
select name, metadata->>'mimetype' as mime, (metadata->>'size')::bigint as bytes, created_at
from storage.objects
where bucket_id = 'exam-images' and name like 'audio/%'
order by created_at desc limit 10;

-- ۳) اگر ردیف ۱ 'audio' نداشت، بخش ۲ نسخهٔ V135 اجرا نشده: SQL_NATIVE_MEDIA_COST_V135_STORAGE.sql را دوباره Run کنید.
