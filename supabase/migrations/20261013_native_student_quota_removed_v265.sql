-- V265 — حذف کامل محدودیت نرخ ساخت دانش‌آموز (به درخواست کاربر؛ قبلاً V75.5: ۳۰ در ساعت / ۲۰۰ در روز).
-- امضا و نتیجه همان قبلی می‌ماند تا Edge Function manage-student بدون deploy مجدد کار کند؛
-- رویداد همچنان برای ممیزی ثبت می‌شود ولی هیچ سقفی اعمال نمی‌شود.
create or replace function public.native_consume_student_create_quota(
    p_actor uuid,
    p_amount integer default 1,
    p_hourly integer default 30,
    p_daily integer default 200
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_amount integer := greatest(coalesce(p_amount, 1), 1);
begin
    if p_actor is null then
        return jsonb_build_object('ok', false, 'error', 'شناسه کاربر نامعتبر است');
    end if;
    insert into public.native_student_create_events(actor, amount) values (p_actor, v_amount);
    delete from public.native_student_create_events where created_at < now() - interval '7 days';
    return jsonb_build_object('ok', true, 'remaining_hour', 999999, 'unlimited', true);
end;
$$;
revoke all on function public.native_consume_student_create_quota(uuid, integer, integer, integer) from public, anon, authenticated;
grant execute on function public.native_consume_student_create_quota(uuid, integer, integer, integer) to service_role;
