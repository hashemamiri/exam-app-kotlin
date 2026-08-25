-- V59.2 — سه اصلاح تقویم:
-- ۱) باگ: دانش‌آموز پیام معلم را نمی‌دید. ریشه: cal_month فقط
--    profiles.teacher_id (مالک) را می‌دید؛ دانش‌آموزِ لینک‌شده
--    (teacher_student_links) پیام معلم لینک‌شده را نمی‌گرفت. حالا مجموعهٔ
--    معلم‌ها = مالک + همهٔ لینک‌ها.
-- ۲) معلم برای روزهای گذشته نمی‌تواند پیام بسازد یا ویرایش کند (حذف آزاد).
-- ۳) اعلان «پیام جدید دارید»: جدول دیده‌شدن + RPC فهرست پیام‌های دیده‌نشده
--    و علامت‌زدن.

create or replace function public.cal_month(p_from date, p_to date)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_teacher uuid;
    v_notes jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_from is null or p_to is null or p_to < p_from or p_to - p_from > 62 then
        return jsonb_build_object('error', 'بازه تقویم نامعتبر است');
    end if;

    select role, teacher_id into v_role, v_teacher from public.profiles where id = v_uid;
    if v_role = 'teacher' then
        select coalesce(jsonb_agg(jsonb_build_object(
            'id', n.id, 'on_date', n.on_date, 'title', n.title,
            'body', n.body, 'audience', n.audience
        ) order by n.on_date, n.created_at), '[]'::jsonb)
        into v_notes
        from public.calendar_notes n
        where n.teacher_id = v_uid and n.on_date between p_from and p_to;
    else
        select coalesce(jsonb_agg(t.item order by t.on_date, t.created_at), '[]'::jsonb)
        into v_notes
        from (
            select distinct n.on_date, n.created_at, jsonb_build_object(
                'id', n.id, 'on_date', n.on_date, 'title', n.title, 'body', n.body
            ) as item
            from public.calendar_notes n
            where n.on_date between p_from and p_to
              -- V59.2: مالک، معلمِ لینک‌شده، یا معلمِ کلاسی که دانش‌آموز عضو آن است
              and (
                n.teacher_id = v_teacher
                or exists (
                    select 1 from public.teacher_student_links l
                    where l.student_id = v_uid and l.teacher_id = n.teacher_id
                )
                or exists (
                    select 1 from public.class_members m
                    join public.classes c on c.id = m.class_id
                    where m.student_id = v_uid and c.teacher_id = n.teacher_id
                )
              )
              and (
                n.audience = 'all'
                or (n.audience = 'students' and exists (
                    select 1 from public.calendar_note_students s
                    where s.note_id = n.id and s.student_id = v_uid
                ))
                or (n.audience = 'classes' and exists (
                    select 1
                    from public.calendar_note_classes c
                    join public.class_members m on m.class_id = c.class_id
                    where c.note_id = n.id and m.student_id = v_uid
                ))
              )
        ) t;
    end if;

    return jsonb_build_object('ok', true, 'notes', coalesce(v_notes, '[]'::jsonb));
end;
$$;

-- ۲) قفل گذشته: بازنویسی cal_save_note با نگه‌داشتن امضا و قواعد قبلی.
create or replace function public.cal_save_note(
    p_date date,
    p_title text,
    p_body text default null,
    p_audience text default 'all',
    p_classes uuid[] default null,
    p_students uuid[] default null,
    p_id uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_id uuid;
    v_mode text := coalesce(p_audience, 'all');
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم می‌تواند پیام ثبت کند');
    end if;
    if p_date is null or p_date < date '2021-03-21' or p_date > date '2122-03-21' then
        return jsonb_build_object('error', 'تاریخ خارج از بازه تقویم است');
    end if;
    -- V59.2 — ساخت/ویرایش پیام فقط برای امروز و آینده؛ حذف همچنان آزاد است.
    if p_date < current_date then
        return jsonb_build_object('error', 'برای روزهای گذشته فقط حذف پیام ممکن است');
    end if;
    if coalesce(btrim(p_title), '') = '' then return jsonb_build_object('error', 'عنوان پیام لازم است'); end if;
    if length(btrim(p_title)) > 120 then return jsonb_build_object('error', 'عنوان حداکثر ۱۲۰ نویسه است'); end if;
    if length(coalesce(p_body, '')) > 2000 then return jsonb_build_object('error', 'توضیحات حداکثر ۲۰۰۰ نویسه است'); end if;
    if v_mode not in ('all', 'classes', 'students') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;

    -- اعتبارسنجی مخاطبان (همان قواعد نسخهٔ اصلی V-calendar).
    if v_mode = 'classes' then
        if coalesce(cardinality(p_classes), 0) = 0 then return jsonb_build_object('error', 'حداقل یک کلاس انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_classes) x
            where not exists (select 1 from public.classes c where c.id = x and c.teacher_id = v_uid)
        ) then return jsonb_build_object('error', 'یک کلاس انتخابی متعلق به این معلم نیست'); end if;
    elsif v_mode = 'students' then
        if coalesce(cardinality(p_students), 0) = 0 then return jsonb_build_object('error', 'حداقل یک دانش‌آموز انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_students) x
            where not exists (
                select 1 from public.profiles p
                where p.id = x and p.role = 'student'
                  and (p.teacher_id = v_uid or exists (
                      select 1 from public.teacher_student_links l
                      where l.teacher_id = v_uid and l.student_id = x
                  ))
            )
        ) then return jsonb_build_object('error', 'یک دانش‌آموز انتخابی متعلق به این معلم نیست'); end if;
    end if;

    if p_id is null then
        insert into public.calendar_notes(teacher_id, on_date, title, body, audience)
        values (v_uid, p_date, btrim(p_title), nullif(btrim(coalesce(p_body, '')), ''), v_mode)
        returning id into v_id;
    else
        -- V59.2 — ویرایش پیامِ روز گذشته مسدود است (فقط حذف).
        if exists (
            select 1 from public.calendar_notes
            where id = p_id and teacher_id = v_uid and on_date < current_date
        ) then
            return jsonb_build_object('error', 'برای روزهای گذشته فقط حذف پیام ممکن است');
        end if;
        update public.calendar_notes
        set on_date = p_date,
            title = btrim(p_title),
            body = nullif(btrim(coalesce(p_body, '')), ''),
            audience = v_mode,
            updated_at = now()
        where id = p_id and teacher_id = v_uid
        returning id into v_id;
        if v_id is null then return jsonb_build_object('error', 'پیام یافت نشد'); end if;
        delete from public.calendar_note_classes where note_id = v_id;
        delete from public.calendar_note_students where note_id = v_id;
    end if;

    if v_mode = 'classes' then
        insert into public.calendar_note_classes(note_id, class_id)
        select v_id, x from (select distinct unnest(p_classes) as x) s
        on conflict do nothing;
    elsif v_mode = 'students' then
        insert into public.calendar_note_students(note_id, student_id)
        select v_id, x from (select distinct unnest(p_students) as x) s
        on conflict do nothing;
    end if;

        return jsonb_build_object('ok', true, 'id', v_id);
end;
$$;

-- ۳) اعلان پیام جدید برای دانش‌آموز.
create table if not exists public.native_calendar_seen (
    student_id uuid not null references auth.users(id) on delete cascade,
    note_id uuid not null references public.calendar_notes(id) on delete cascade,
    seen_at timestamptz not null default now(),
    primary key (student_id, note_id)
);
alter table public.native_calendar_seen enable row level security;
drop policy if exists native_calendar_seen_own on public.native_calendar_seen;
create policy native_calendar_seen_own on public.native_calendar_seen
    for all using (student_id = auth.uid()) with check (student_id = auth.uid());

create or replace function public.cal_unseen_v59()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_teacher uuid;
    v_rows jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select teacher_id into v_teacher from public.profiles where id = v_uid and role = 'student';
    if not found then return jsonb_build_object('ok', true, 'notes', '[]'::jsonb); end if;

    select coalesce(jsonb_agg(jsonb_build_object(
        'id', n.id, 'on_date', n.on_date, 'title', n.title, 'body', n.body
    ) order by n.created_at desc), '[]'::jsonb)
    into v_rows
    from public.calendar_notes n
    where n.created_at > now() - interval '14 days'
      and not exists (
          select 1 from public.native_calendar_seen s
          where s.student_id = v_uid and s.note_id = n.id
      )
      and (
        n.teacher_id = v_teacher
        or exists (
            select 1 from public.teacher_student_links l
            where l.student_id = v_uid and l.teacher_id = n.teacher_id
        )
        or exists (
            select 1 from public.class_members m
            join public.classes c on c.id = m.class_id
            where m.student_id = v_uid and c.teacher_id = n.teacher_id
        )
      )
      and (
        n.audience = 'all'
        or (n.audience = 'students' and exists (
            select 1 from public.calendar_note_students s
            where s.note_id = n.id and s.student_id = v_uid
        ))
        or (n.audience = 'classes' and exists (
            select 1 from public.calendar_note_classes c
            join public.class_members m on m.class_id = c.class_id
            where c.note_id = n.id and m.student_id = v_uid
        ))
      );

    return jsonb_build_object('ok', true, 'notes', coalesce(v_rows, '[]'::jsonb));
end;
$$;

create or replace function public.cal_mark_seen_v59(p_note uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_note is null then return jsonb_build_object('error', 'شناسه پیام لازم است'); end if;
    insert into public.native_calendar_seen(student_id, note_id)
    values (v_uid, p_note)
    on conflict do nothing;
    return jsonb_build_object('ok', true);
end;
$$;

revoke all on function public.cal_unseen_v59() from public, anon;
grant execute on function public.cal_unseen_v59() to authenticated;
revoke all on function public.cal_mark_seen_v59(uuid) from public, anon;
grant execute on function public.cal_mark_seen_v59(uuid) to authenticated;


-- سلامت‌سنجی پس از اجرا: این کوئری باید true بدهد (پوشش کلاس فعال است):
-- select position('class_members' in pg_get_functiondef('public.cal_month(date,date)'::regprocedure)) > 0;
