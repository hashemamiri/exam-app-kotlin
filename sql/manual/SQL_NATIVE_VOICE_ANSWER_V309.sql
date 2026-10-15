-- V309 / ۱۰ — پاسخ صوتی دانش‌آموز (idempotent)
-- ضبط مرورگر (Chrome/Firefox) معمولاً webm یا ogg است؛ اپ اندروید m4a می‌فرستد.
-- این اسکریپت فقط نوع‌های صوتی را به فهرست مجاز bucket «exam-images» اضافه می‌کند.
-- اگر فهرست خالی یا تهی باشد (یعنی همه مجازند) تغییری داده نمی‌شود.
DO $$
DECLARE
  v_wanted text[] := ARRAY['audio/webm', 'audio/ogg', 'audio/x-m4a', 'audio/mp4'];
  v_current text[];
  v_t text;
BEGIN
  SELECT allowed_mime_types INTO v_current FROM storage.buckets WHERE id = 'exam-images';
  IF v_current IS NULL OR cardinality(v_current) = 0 THEN
    RAISE NOTICE 'exam-images: فهرست نوع‌های مجاز خالی است؛ تغییری لازم نیست.';
    RETURN;
  END IF;
  FOREACH v_t IN ARRAY v_wanted LOOP
    IF NOT (v_t = ANY (v_current)) THEN
      v_current := array_append(v_current, v_t);
    END IF;
  END LOOP;
  UPDATE storage.buckets SET allowed_mime_types = v_current WHERE id = 'exam-images';
END $$;

SELECT id, allowed_mime_types FROM storage.buckets WHERE id = 'exam-images';
