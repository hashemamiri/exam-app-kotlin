-- V305 — کارنامهٔ توصیفی ۴ سطحی (حالت ابتدایی).
-- فقط آزمون‌های primary؛ سطح از آستانه‌های exams.options.levels ([85,70,50] پیش‌فرض) محاسبه می‌شود.
-- دانش‌آموز فقط سطح و ستاره می‌بیند؛ آستانه‌ها و درصد خام در خروجی نیست (فقط برای خود دانش‌آموز).
-- بدون تغییر در جدول‌ها و بدون تغییر در تصحیح خودکار.

create or replace function public.native_my_levels_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    with g as (
        select distinct on (a.exam_id)
            a.exam_id,
            coalesce(a.total_grade, 0) as grade,
            coalesce(public.native_answer_max_score_v255(a.meta), e.total_score, 0) as max_score,
            e.options
        from public.answers a
        join public.exams e on e.id = a.exam_id
        where auth.uid() is not null
          and a.student_id = auth.uid()
          and coalesce(a.graded, false)
          and coalesce(e.options ->> 'primary', 'false') = 'true'
        order by a.exam_id, a.submitted_at desc
    ), p as (
        select
            g.exam_id,
            round(g.grade * 100.0 / nullif(g.max_score, 0), 2) as pct,
            coalesce(nullif(g.options -> 'levels' ->> 0, '')::numeric, 85) as t1,
            coalesce(nullif(g.options -> 'levels' ->> 1, '')::numeric, 70) as t2,
            coalesce(nullif(g.options -> 'levels' ->> 2, '')::numeric, 50) as t3
        from g
    ), r as (
        select
            p.exam_id,
            p.pct,
            case
                when p.pct >= p.t1 then 4
                when p.pct >= p.t2 then 3
                when p.pct >= p.t3 then 2
                else 1
            end as rank
        from p
        where p.pct is not null
    )
    select jsonb_build_object(
        'ok', true,
        'items', coalesce(jsonb_agg(
            jsonb_build_object(
                'exam_id', r.exam_id,
                'level_rank', r.rank,
                'level_name', case r.rank
                    when 4 then 'خیلی خوب'
                    when 3 then 'خوب'
                    when 2 then 'قابل قبول'
                    else 'نیاز به تلاش بیشتر'
                end
            )
        ), '[]'::jsonb)
    )
    from r;
$$;

revoke all on function public.native_my_levels_v1() from public, anon;
grant execute on function public.native_my_levels_v1() to authenticated;
