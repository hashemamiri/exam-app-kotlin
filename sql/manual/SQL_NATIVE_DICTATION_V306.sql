-- ============================================================
-- V306 — دستی: کپی همین فایل را در SQL Editor سوپابیس اجرا کنید.
-- ============================================================
-- V306 — املا و پاسخ با تخته (حالت ابتدایی): نرمال‌سازی تصحیح جای خالی.
-- فقط تابع نرمال‌سازی تغییر می‌کند (جدول و داده دست‌نخورده): نیم‌فاصله (ZWNJ) و فاصله‌ها حذف می‌شوند،
-- «ة» مثل «ه» و «ي/ك» مثل «ی/ک» یکی می‌شوند. کلیدهای قبلی بدون تغییر در معنا، فقط کمی بخشنده‌تر.
-- اجرای دوباره بی‌خطر است (create or replace).

create or replace function public.native_normalize_text_v1(p text)
returns text
language sql
immutable
as $$
    select regexp_replace(
        lower(trim(translate(coalesce(p, ''),
            '۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩يكۀة٫،',
            '01234567890123456789یکهه.,'))),
        '[[:space:]' || chr(8204) || chr(8205) || ']+', '', 'g');
$$;

revoke all on function public.native_normalize_text_v1(text) from public, anon;
grant execute on function public.native_normalize_text_v1(text) to authenticated;

notify pgrst, 'reload schema';
