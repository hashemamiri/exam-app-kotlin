-- V58.1 — گزارش نظارتی آزمون (فقط برای معلم مالک آزمون)
-- دانش‌آموز گزارش خودش را می‌نویسد (upsert)؛ فقط معلمِ همان آزمون می‌خواند.
-- هیچ دادهٔ حساسی ذخیره نمی‌شود: فقط شمارندهٔ رویدادها و زمان‌بندی سؤال‌ها.

create table if not exists public.native_exam_monitor (
    exam_id text not null,
    student_id uuid not null references auth.users(id) on delete cascade,
    report jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (exam_id, student_id)
);

alter table public.native_exam_monitor enable row level security;

drop policy if exists native_exam_monitor_owner_rw on public.native_exam_monitor;
create policy native_exam_monitor_owner_rw on public.native_exam_monitor
    for all using (student_id = auth.uid()) with check (student_id = auth.uid());

-- دانش‌آموز: ثبت/به‌روزرسانی گزارش خودش (ادغام jsonb تا رویدادهای قبلی نپرند)
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
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if coalesce(btrim(p_exam), '') = '' then return jsonb_build_object('error', 'شناسه آزمون لازم است'); end if;
    if p_report is null or jsonb_typeof(p_report) <> 'object' then
        return jsonb_build_object('error', 'گزارش نامعتبر است');
    end if;

    insert into public.native_exam_monitor(exam_id, student_id, report, updated_at)
    values (p_exam, v_uid, p_report, now())
    on conflict (exam_id, student_id)
    do update set report = public.native_exam_monitor.report || excluded.report,
                  updated_at = now();
    return jsonb_build_object('ok', true);
end;
$$;

-- معلم: خواندن گزارش‌های آزمونِ خودش همراه نام دانش‌آموز
create or replace function public.native_monitor_list_v1(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_rows jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (
        select 1 from public.exams e
        where e.id = p_exam and e.teacher_id = v_uid
    ) then
        return jsonb_build_object('error', 'این آزمون متعلق به شما نیست');
    end if;

    select coalesce(jsonb_agg(jsonb_build_object(
        'student_id', m.student_id,
        'student_name', coalesce(nullif(p.full_name, ''), nullif(p.display_name, ''), 'دانش‌آموز'),
        'report', m.report,
        'updated_at', m.updated_at
    ) order by m.updated_at desc), '[]'::jsonb)
    into v_rows
    from public.native_exam_monitor m
    left join public.profiles p on p.id = m.student_id
    where m.exam_id = p_exam;

    return jsonb_build_object('rows', v_rows);
end;
$$;

grant execute on function public.native_monitor_upsert_v1(text, jsonb) to authenticated;
grant execute on function public.native_monitor_list_v1(text) to authenticated;
