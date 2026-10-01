-- V201 — هش سربرگ پرداخت چاپ از سربرگ «پروفایل سرور» (استان/شهر/منطقه/مدرسه/پایه/رشته) + درس و مدت آزمون چاپی،
-- نه از فیلدهای محلی دستگاه (PrintHeaderStore اپ / localStorage سایت) — تا پرداخت در سایت و چاپ در اپ (یا دستگاه دیگر)
-- «تغییر سربرگ» حساب نشود. امضای تابع همان است؛ p_header نادیده گرفته می‌شود (کلاینت‌ها بدون تغییر).
-- توجه: پرداخت‌های انجام‌شده با هش قدیمی یک‌بار «سربرگ تغییر کرده» می‌شوند.
begin;
create or replace function public.native_print_header_hash_v199(p_exam public.print_exams, p_header text)
returns text
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select md5(
        coalesce(p.hdr_province, '') || '|' || coalesce(p.hdr_city, '') || '|' || coalesce(p.hdr_district, '') || '|' ||
        coalesce(p.hdr_school, '') || '|' || coalesce(p.hdr_grade, '') || '|' || coalesce(p.hdr_field, '') || '|' ||
        coalesce(p_exam.subject, '') || '|' || coalesce(p_exam.duration, 0)::text
    )
    from public.profiles p where p.id = p_exam.teacher_id;
$$;
revoke all on function public.native_print_header_hash_v199(public.print_exams, text) from public, anon, authenticated;
commit;
notify pgrst, 'reload schema';
