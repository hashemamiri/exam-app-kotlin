-- V75.5 — محدودیت نرخِ ساخت حساب دانش‌آموز (بند ۳.۶ گزارش امنیتی).
-- ریشه: Edge Function مدیریت دانش‌آموز action=create و action=bulk هیچ سقفی نداشت؛
-- یک معلم/مهاجمِ دارای نشست معتبر می‌توانست در چند دقیقه هزاران حساب در Auth و
-- profiles بسازد (هزینهٔ مستقیم اشغال ظرفیت و اخلال در مدرسه).
-- راهکار: شمارش در دیتابیس (پایدار در برابر سردشدن Edge و چند instance) با قفل مشورتی،
-- نه شمارندهٔ حافظه‌ای که با ری‌استارت تابع صفر می‌شود.

begin;

create table if not exists public.native_student_create_events (
    id bigserial primary key,
    actor uuid not null,
    amount integer not null default 1,
    created_at timestamptz not null default now()
);

create index if not exists idx_native_student_create_events_actor
    on public.native_student_create_events(actor, created_at desc);

alter table public.native_student_create_events enable row level security;

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
    v_hour integer := 0;
    v_day integer := 0;
begin
    if p_actor is null then
        return jsonb_build_object('ok', false, 'error', 'شناسه کاربر نامعتبر است');
    end if;

    -- قفل مشورتیِ تراکنشی: دو فراخوانی هم‌زمانِ یک کاربر نمی‌توانند هر دو عبور کنند.
    perform pg_advisory_xact_lock(hashtextextended('student-create:' || p_actor::text, 472871));

    select
        coalesce(sum(e.amount) filter (where e.created_at > now() - interval '1 hour'), 0),
        coalesce(sum(e.amount) filter (where e.created_at > now() - interval '24 hours'), 0)
    into v_hour, v_day
    from public.native_student_create_events e
    where e.actor = p_actor;

    if v_hour + v_amount > p_hourly then
        return jsonb_build_object('ok', false, 'error', 'سقف ساخت دانش‌آموز در یک ساعت پر شده است؛ کمی بعد دوباره تلاش کنید');
    end if;
    if v_day + v_amount > p_daily then
        return jsonb_build_object('ok', false, 'error', 'سقف ساخت دانش‌آموز در ۲۴ ساعت پر شده است؛ فردا دوباره تلاش کنید');
    end if;

    insert into public.native_student_create_events(actor, amount) values (p_actor, v_amount);
    delete from public.native_student_create_events where created_at < now() - interval '7 days';

    return jsonb_build_object('ok', true, 'remaining_hour', p_hourly - v_hour - v_amount);
end;
$$;

revoke all on function public.native_consume_student_create_quota(uuid, integer, integer, integer)
    from public, anon, authenticated;
grant execute on function public.native_consume_student_create_quota(uuid, integer, integer, integer)
    to service_role;

commit;
