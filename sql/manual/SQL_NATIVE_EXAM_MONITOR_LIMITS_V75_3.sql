-- V75.3 — محافظ مانیتورینگ آزمون (بند ۳.۲ گزارش امنیتی).
-- ریشه: native_monitor_upsert_v1 فقط auth.uid() را چک می‌کرد؛ بنابراین
--   الف) هر کاربر می‌توانست برای هر exam_id دلخواه (حتی ناموجود) ردیف بسازد،
--   ب) اندازهٔ p_report محدود نبود و با عملگر || روی گزارش قبلی جمع می‌شد
--      ⇒ رشد نامحدود دیتابیس و امکان آلوده‌کردن گزارش معلم با دادهٔ جعلی.
-- این فایل سه گارد اضافه می‌کند و رفتار عادی را تغییر نمی‌دهد.

begin;

create or replace function public.native_monitor_upsert_v1(
    p_exam text,
    p_report jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_len integer := 0;
    v_keys integer := 0;
    v_existing integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if coalesce(btrim(p_exam), '') = '' then return jsonb_build_object('error', 'شناسه آزمون لازم است'); end if;
    if p_report is null or jsonb_typeof(p_report) <> 'object' then
        return jsonb_build_object('error', 'گزارش نامعتبر است');
    end if;

    -- V75.3 — کنترل وجود آزمون: هر کاربر واردشده‌ای نمی‌تواند برای یک شناسهٔ
    -- ساختگی ردیف بسازد و پایگاه را با کلیدهای دلخواه پر کند.
    if not exists (select 1 from public.exams e where e.id = p_exam) then
        return jsonb_build_object('error', 'آزمون یافت نشد');
    end if;

    -- V75.3 — محدودیت اندازه: گزارش تکی و مجموعِ ذخیره‌شده برای هر دانش‌آموز.
    v_len := octet_length(p_report::text);
    if v_len > 8192 then
        return jsonb_build_object('error', 'حجم گزارش بیش از حد مجاز است');
    end if;
    select count(*) into v_keys from jsonb_object_keys(p_report);
    if v_keys > 100 then
        return jsonb_build_object('error', 'تعداد آیتم‌های گزارش بیش از حد مجاز است');
    end if;
    select octet_length(coalesce(m.report::text, '{}')) into v_existing
    from public.native_exam_monitor m
    where m.exam_id = p_exam and m.student_id = v_uid;
    if coalesce(v_existing, 0) + v_len > 32768 then
        return jsonb_build_object('error', 'مجموع گزارش این آزمون از سقف مجاز گذشته است');
    end if;

    insert into public.native_exam_monitor(exam_id, student_id, report, updated_at)
    values (p_exam, v_uid, p_report, now())
    on conflict (exam_id, student_id)
    do update set report = public.native_exam_monitor.report || excluded.report,
                  updated_at = now();
    return jsonb_build_object('ok', true);
end;
$$;

revoke all on function public.native_monitor_upsert_v1(text, jsonb) from public, anon;
grant execute on function public.native_monitor_upsert_v1(text, jsonb) to authenticated;

commit;
