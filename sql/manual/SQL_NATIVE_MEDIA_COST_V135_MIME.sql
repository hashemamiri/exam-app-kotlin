-- V135.4 — خطای «mime type audio/mp4 is not supported» هنگام ذخیرهٔ آزمون با فایل صوتی:
-- باکت exam-images فقط تصویر را مجاز داشت. صوتِ فشرده‌شدهٔ سؤال (AAC در ظرف m4a)
-- با نوع audio/mp4 آپلود می‌شود؛ این نوع‌ها به فهرست مجاز اضافه می‌شوند.
-- (نوع‌های موجود حفظ می‌شوند؛ اگر فهرست خالی/نامحدود بود، همچنان نامحدود می‌ماند.)
begin;
set local lock_timeout = '8s';

update storage.buckets
set allowed_mime_types = (
    select array_agg(distinct m)
    from unnest(
        coalesce(allowed_mime_types, array[]::text[])
        || array['audio/mp4', 'audio/x-m4a', 'audio/aac', 'audio/mpeg']
    ) as m
)
where id = 'exam-images'
  and allowed_mime_types is not null
  and cardinality(allowed_mime_types) > 0;

commit;

-- بررسی: select id, allowed_mime_types, file_size_limit from storage.buckets where id = 'exam-images';
