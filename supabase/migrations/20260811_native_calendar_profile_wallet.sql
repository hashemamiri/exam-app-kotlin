-- V9: تقویم جلالی/پیام هدف‌دار، پروفایل/سربرگ و کیف پول/پرداخت امن
-- مبنا: schema زنده پروژه eazwuyrymsvdkwckdpco در 2026-08-11
-- این migration چندباراجرا (idempotent) و با safeupdate سازگار است.

begin;

-- ============================================================
-- 1) پروفایل، آواتار و سربرگ
-- ============================================================
alter table public.profiles add column if not exists avatar_url text;
alter table public.profiles add column if not exists avatar_public boolean not null default true;
alter table public.profiles add column if not exists hdr_province text;
alter table public.profiles add column if not exists hdr_city text;
alter table public.profiles add column if not exists hdr_district text;
alter table public.profiles add column if not exists hdr_school text;

create or replace function public.native_my_profile()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce((
        select jsonb_build_object(
            'ok', true,
            'id', p.id,
            'full_name', p.full_name,
            'display_name', p.display_name,
            'username', p.username,
            'role', p.role,
            'avatar_url', p.avatar_url,
            'avatar_public', coalesce(p.avatar_public, true),
            'hdr_province', p.hdr_province,
            'hdr_city', p.hdr_city,
            'hdr_district', p.hdr_district,
            'hdr_school', p.hdr_school
        )
        from public.profiles p
        where p.id = auth.uid()
    ), jsonb_build_object('error', 'پروفایل یافت نشد'));
$$;
revoke all on function public.native_my_profile() from public, anon;
grant execute on function public.native_my_profile() to authenticated;

create or replace function public.native_save_profile(
    p_display_name text,
    p_avatar_url text,
    p_avatar_public boolean,
    p_hdr_province text,
    p_hdr_city text,
    p_hdr_district text,
    p_hdr_school text
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_old_avatar text;
    v_avatar text := nullif(btrim(coalesce(p_avatar_url, '')), '');
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;

    select role, avatar_url into v_role, v_old_avatar
    from public.profiles
    where id = v_uid;
    if not found then
        return jsonb_build_object('error', 'پروفایل یافت نشد');
    end if;

    if length(coalesce(p_display_name, '')) > 100 then
        return jsonb_build_object('error', 'نام نمایشی حداکثر ۱۰۰ نویسه است');
    end if;
    if greatest(
        length(coalesce(p_hdr_province, '')),
        length(coalesce(p_hdr_city, '')),
        length(coalesce(p_hdr_district, '')),
        length(coalesce(p_hdr_school, ''))
    ) > 120 then
        return jsonb_build_object('error', 'هر بخش سربرگ حداکثر ۱۲۰ نویسه است');
    end if;
    if v_avatar is not null and length(v_avatar) > 2048 then
        return jsonb_build_object('error', 'نشانی آواتار بیش از حد بلند است');
    end if;
    -- آواتار جدید Native فقط در شاخه مالک ذخیره می‌شود. آواتار قدیمی بدون تغییر پذیرفته می‌شود.
    if v_avatar is not null
       and v_avatar is distinct from v_old_avatar
       and position('/storage/v1/object/public/exam-images/avatars/' || v_uid::text || '/' in v_avatar) = 0 then
        return jsonb_build_object('error', 'مسیر آواتار با حساب فعلی مطابقت ندارد');
    end if;

    update public.profiles
    set display_name = case
            when v_role = 'teacher' then nullif(btrim(coalesce(p_display_name, '')), '')
            else display_name
        end,
        avatar_url = v_avatar,
        avatar_public = coalesce(p_avatar_public, true),
        hdr_province = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_province, '')), '') else hdr_province end,
        hdr_city = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_city, '')), '') else hdr_city end,
        hdr_district = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_district, '')), '') else hdr_district end,
        hdr_school = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_school, '')), '') else hdr_school end
    where id = v_uid;

    return jsonb_build_object('ok', true, 'avatar_url', v_avatar);
end;
$$;
revoke all on function public.native_save_profile(text,text,boolean,text,text,text,text) from public, anon;
grant execute on function public.native_save_profile(text,text,boolean,text,text,text,text) to authenticated;

create or replace function public.teacher_public_profile()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_teacher uuid;
    v_result jsonb;
begin
    select teacher_id into v_teacher
    from public.profiles
    where id = auth.uid() and role = 'student';

    if v_teacher is null then
        return jsonb_build_object('ok', false);
    end if;

    select jsonb_build_object(
        'ok', true,
        'name', coalesce(nullif(btrim(coalesce(p.display_name, '')), ''), p.full_name),
        'avatar', case when coalesce(p.avatar_public, true) then p.avatar_url else null end
    ) into v_result
    from public.profiles p
    where p.id = v_teacher and p.role = 'teacher';

    return coalesce(v_result, jsonb_build_object('ok', false));
end;
$$;
revoke all on function public.teacher_public_profile() from public, anon;
grant execute on function public.teacher_public_profile() to authenticated;

-- ============================================================
-- 2) تقویم و پیام هدف‌دار
-- ============================================================
create table if not exists public.calendar_notes (
    id uuid primary key default gen_random_uuid(),
    teacher_id uuid not null references auth.users(id) on delete cascade,
    on_date date not null,
    title text not null,
    body text,
    audience text not null default 'all' check (audience in ('all', 'classes', 'students')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_cal_teacher_date on public.calendar_notes(teacher_id, on_date);

create table if not exists public.calendar_note_classes (
    note_id uuid not null references public.calendar_notes(id) on delete cascade,
    class_id uuid not null references public.classes(id) on delete cascade,
    primary key (note_id, class_id)
);
create table if not exists public.calendar_note_students (
    note_id uuid not null references public.calendar_notes(id) on delete cascade,
    student_id uuid not null references public.profiles(id) on delete cascade,
    primary key (note_id, student_id)
);

alter table public.calendar_notes enable row level security;
alter table public.calendar_note_classes enable row level security;
alter table public.calendar_note_students enable row level security;

drop policy if exists p_cal_own on public.calendar_notes;
create policy p_cal_own on public.calendar_notes
for all to authenticated
using (teacher_id = auth.uid())
with check (teacher_id = auth.uid());

drop policy if exists p_cal_cls_own on public.calendar_note_classes;
create policy p_cal_cls_own on public.calendar_note_classes
for all to authenticated
using (exists (
    select 1 from public.calendar_notes n
    where n.id = note_id and n.teacher_id = auth.uid()
))
with check (exists (
    select 1 from public.calendar_notes n
    where n.id = note_id and n.teacher_id = auth.uid()
));

drop policy if exists p_cal_stu_own on public.calendar_note_students;
create policy p_cal_stu_own on public.calendar_note_students
for all to authenticated
using (exists (
    select 1 from public.calendar_notes n
    where n.id = note_id and n.teacher_id = auth.uid()
))
with check (exists (
    select 1 from public.calendar_notes n
    where n.id = note_id and n.teacher_id = auth.uid()
));

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
    if coalesce(btrim(p_title), '') = '' then return jsonb_build_object('error', 'عنوان پیام لازم است'); end if;
    if length(btrim(p_title)) > 120 then return jsonb_build_object('error', 'عنوان حداکثر ۱۲۰ نویسه است'); end if;
    if length(coalesce(p_body, '')) > 2000 then return jsonb_build_object('error', 'توضیحات حداکثر ۲۰۰۰ نویسه است'); end if;
    if v_mode not in ('all', 'classes', 'students') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;

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
                where p.id = x and p.teacher_id = v_uid and p.role = 'student'
            )
        ) then return jsonb_build_object('error', 'یک دانش‌آموز انتخابی متعلق به این معلم نیست'); end if;
    end if;

    if p_id is null then
        insert into public.calendar_notes(teacher_id, on_date, title, body, audience)
        values (v_uid, p_date, btrim(p_title), nullif(btrim(coalesce(p_body, '')), ''), v_mode)
        returning id into v_id;
    else
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
revoke all on function public.cal_save_note(date,text,text,text,uuid[],uuid[],uuid) from public, anon;
grant execute on function public.cal_save_note(date,text,text,text,uuid[],uuid[],uuid) to authenticated;

create or replace function public.cal_delete_note(p_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_count integer;
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    delete from public.calendar_notes where id = p_id and teacher_id = auth.uid();
    get diagnostics v_count = row_count;
    if v_count = 0 then return jsonb_build_object('error', 'پیام یافت نشد'); end if;
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.cal_delete_note(uuid) from public, anon;
grant execute on function public.cal_delete_note(uuid) to authenticated;

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
            where n.teacher_id = v_teacher
              and n.on_date between p_from and p_to
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
revoke all on function public.cal_month(date,date) from public, anon;
grant execute on function public.cal_month(date,date) to authenticated;

create or replace function public.cal_day(p_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_note public.calendar_notes%rowtype;
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select * into v_note
    from public.calendar_notes
    where id = p_id and teacher_id = auth.uid();
    if not found then return jsonb_build_object('error', 'پیام یافت نشد'); end if;

    return jsonb_build_object(
        'ok', true,
        'id', v_note.id,
        'on_date', v_note.on_date,
        'title', v_note.title,
        'body', v_note.body,
        'audience', v_note.audience,
        'classes', (select coalesce(jsonb_agg(class_id), '[]'::jsonb) from public.calendar_note_classes where note_id = v_note.id),
        'students', (select coalesce(jsonb_agg(student_id), '[]'::jsonb) from public.calendar_note_students where note_id = v_note.id)
    );
end;
$$;
revoke all on function public.cal_day(uuid) from public, anon;
grant execute on function public.cal_day(uuid) to authenticated;

-- ============================================================
-- 3) تعطیلات رسمی سرورمحور
-- ============================================================
create table if not exists public.holidays (
    id bigserial primary key,
    on_date date not null unique,
    jy integer not null,
    jm integer not null,
    jd integer not null,
    title text not null,
    is_holiday boolean not null default true,
    source text default 'official',
    created_at timestamptz not null default now()
);
create index if not exists idx_holidays_jy on public.holidays(jy);
create index if not exists idx_holidays_range on public.holidays(on_date);

create table if not exists public.holiday_years (
    jy integer primary key,
    is_exact boolean not null default true,
    note text,
    updated_at timestamptz not null default now()
);

alter table public.holidays enable row level security;
alter table public.holiday_years enable row level security;
drop policy if exists p_holidays_read on public.holidays;
create policy p_holidays_read on public.holidays for select to authenticated, anon using (true);
drop policy if exists p_holiday_years_read on public.holiday_years;
create policy p_holiday_years_read on public.holiday_years for select to authenticated, anon using (true);

create or replace function public.holidays_for(p_from date, p_to date)
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select case
        when p_from is null or p_to is null or p_to < p_from or p_to - p_from > 62
            then jsonb_build_object('error', 'بازه تعطیلات نامعتبر است')
        else jsonb_build_object(
            'ok', true,
            'days', coalesce((
                select jsonb_agg(jsonb_build_object(
                    'jy', h.jy, 'jm', h.jm, 'jd', h.jd,
                    'title', h.title, 'holiday', h.is_holiday
                ) order by h.on_date)
                from public.holidays h
                where h.on_date between p_from and p_to
            ), '[]'::jsonb),
            'years', coalesce((
                select jsonb_object_agg(y.jy::text, y.is_exact)
                from public.holiday_years y
            ), '{}'::jsonb)
        )
    end;
$$;
revoke all on function public.holidays_for(date,date) from public;
grant execute on function public.holidays_for(date,date) to authenticated, anon;

-- داده رسمی ۱۴۰۳ تا ۱۴۰۵ از migration واقعی WebView، بدون محاسبه یا حدس.
insert into public.holiday_years(jy, is_exact, note) values
  (1403, true, 'تقویم رسمی'),
  (1404, true, 'تقویم رسمی'),
  (1405, true, 'تقویم رسمی')
on conflict (jy) do update set is_exact = excluded.is_exact, updated_at = now();

insert into public.holidays(on_date, jy, jm, jd, title) values
  -- ================= ۱۴۰۳ =================
  ('2024-03-20',1403,1,1,'آغاز نوروز'),
  ('2024-03-21',1403,1,2,'عید نوروز'),
  ('2024-03-22',1403,1,3,'عید نوروز'),
  ('2024-03-23',1403,1,4,'عید نوروز'),
  ('2024-03-31',1403,1,12,'روز جمهوری اسلامی ایران'),
  ('2024-04-01',1403,1,13,'روز طبیعت (سیزده‌به‌در) — شهادت حضرت علی (ع)'),
  ('2024-04-10',1403,1,22,'عید سعید فطر'),
  ('2024-04-11',1403,1,23,'تعطیل به مناسبت عید سعید فطر'),
  ('2024-05-04',1403,2,15,'شهادت امام جعفر صادق (ع)'),
  ('2024-06-03',1403,3,14,'رحلت امام خمینی (ره)'),
  ('2024-06-04',1403,3,15,'قیام ۱۵ خرداد'),
  ('2024-06-17',1403,3,28,'عید سعید قربان'),
  ('2024-06-25',1403,4,5,'عید سعید غدیر خم'),
  ('2024-07-15',1403,4,25,'تاسوعای حسینی'),
  ('2024-07-16',1403,4,26,'عاشورای حسینی'),
  ('2024-08-25',1403,6,4,'اربعین حسینی'),
  ('2024-09-02',1403,6,12,'رحلت پیامبر اکرم (ص) و شهادت امام حسن مجتبی (ع)'),
  ('2024-09-04',1403,6,14,'شهادت امام رضا (ع)'),
  ('2024-09-12',1403,6,22,'شهادت امام حسن عسکری (ع) — آغاز امامت حضرت ولی‌عصر (عج)'),
  ('2024-09-21',1403,6,31,'میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع)'),
  ('2024-12-05',1403,9,15,'شهادت حضرت فاطمه زهرا (س)'),
  ('2025-01-14',1403,10,25,'ولادت امام علی (ع) و روز پدر'),
  ('2025-01-28',1403,11,9,'مبعث پیامبر اکرم (ص)'),
  ('2025-02-10',1403,11,22,'پیروزی انقلاب اسلامی'),
  ('2025-02-14',1403,11,26,'ولادت حضرت قائم (عج) — نیمه شعبان'),
  ('2025-03-19',1403,12,29,'روز ملی شدن صنعت نفت'),
  ('2025-03-20',1403,12,30,'تعطیلات نوروز (۲۹ اسفند تا ۴ فروردین)'),
  -- ================= ۱۴۰۴ =================
  ('2025-03-21',1404,1,1,'آغاز نوروز'),
  ('2025-03-22',1404,1,2,'عید نوروز — شهادت حضرت علی (ع)'),
  ('2025-03-23',1404,1,3,'عید نوروز'),
  ('2025-03-24',1404,1,4,'عید نوروز'),
  ('2025-03-31',1404,1,11,'عید سعید فطر'),
  ('2025-04-01',1404,1,12,'روز جمهوری اسلامی — تعطیل عید فطر'),
  ('2025-04-02',1404,1,13,'روز طبیعت (سیزده‌به‌در)'),
  ('2025-04-24',1404,2,4,'شهادت امام جعفر صادق (ع)'),
  ('2025-06-04',1404,3,14,'رحلت امام خمینی (ره)'),
  ('2025-06-05',1404,3,15,'قیام ۱۵ خرداد'),
  ('2025-06-06',1404,3,16,'عید سعید قربان'),
  ('2025-06-14',1404,3,24,'عید سعید غدیر خم'),
  ('2025-07-05',1404,4,14,'تاسوعای حسینی'),
  ('2025-07-06',1404,4,15,'عاشورای حسینی'),
  ('2025-08-14',1404,5,23,'اربعین حسینی'),
  ('2025-08-22',1404,5,31,'رحلت پیامبر اکرم (ص) و شهادت امام حسن مجتبی (ع)'),
  ('2025-08-24',1404,6,2,'شهادت امام رضا (ع)'),
  ('2025-09-01',1404,6,10,'شهادت امام حسن عسکری (ع)'),
  ('2025-09-10',1404,6,19,'میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع)'),
  ('2025-11-24',1404,9,3,'شهادت حضرت فاطمه زهرا (س)'),
  ('2026-01-03',1404,10,13,'ولادت امام علی (ع) و روز پدر'),
  ('2026-01-17',1404,10,27,'مبعث پیامبر اکرم (ص)'),
  ('2026-02-04',1404,11,15,'ولادت حضرت قائم (عج) — نیمه شعبان'),
  ('2026-02-11',1404,11,22,'پیروزی انقلاب اسلامی'),
  ('2026-03-11',1404,12,20,'شهادت حضرت علی (ع)'),
  ('2026-03-20',1404,12,29,'روز ملی شدن صنعت نفت'),
  -- ================= ۱۴۰۵ =================
  ('2026-03-21',1405,1,1,'جشن نوروز — عید سعید فطر'),
  ('2026-03-22',1405,1,2,'عید نوروز — تعطیل عید فطر'),
  ('2026-03-23',1405,1,3,'عید نوروز'),
  ('2026-03-24',1405,1,4,'عید نوروز'),
  ('2026-04-01',1405,1,12,'روز جمهوری اسلامی ایران'),
  ('2026-04-02',1405,1,13,'روز طبیعت (سیزده‌به‌در)'),
  ('2026-04-14',1405,1,25,'شهادت امام جعفر صادق (ع)'),
  ('2026-05-27',1405,3,6,'عید سعید قربان'),
  ('2026-06-04',1405,3,14,'رحلت امام خمینی (ره) — عید سعید غدیر خم'),
  ('2026-06-05',1405,3,15,'قیام ۱۵ خرداد'),
  ('2026-06-24',1405,4,3,'تاسوعای حسینی'),
  ('2026-06-25',1405,4,4,'عاشورای حسینی'),
  ('2026-08-04',1405,5,13,'اربعین حسینی'),
  ('2026-08-12',1405,5,21,'رحلت پیامبر اکرم (ص) و شهادت امام حسن مجتبی (ع)'),
  ('2026-08-13',1405,5,22,'شهادت امام رضا (ع)'),
  ('2026-08-21',1405,5,30,'شهادت امام حسن عسکری (ع) — آغاز امامت حضرت ولی‌عصر (عج)'),
  ('2026-08-30',1405,6,8,'میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع)'),
  ('2026-11-13',1405,8,22,'شهادت حضرت فاطمه زهرا (س)'),
  ('2026-12-23',1405,10,2,'ولادت امام علی (ع) و روز پدر'),
  ('2027-01-06',1405,10,16,'مبعث پیامبر اکرم (ص)'),
  ('2027-01-24',1405,11,4,'ولادت حضرت قائم (عج) — نیمه شعبان'),
  ('2027-02-11',1405,11,22,'پیروزی انقلاب اسلامی'),
  ('2027-02-28',1405,12,9,'شهادت حضرت علی (ع)'),
  ('2027-03-10',1405,12,19,'عید سعید فطر'),
  ('2027-03-11',1405,12,20,'تعطیل به مناسبت عید فطر'),
  ('2027-03-20',1405,12,29,'روز ملی شدن صنعت نفت')
on conflict (on_date) do update
  set title = excluded.title, jy = excluded.jy, jm = excluded.jm, jd = excluded.jd;


-- ============================================================
-- 4) کیف پول، دفتر تراکنش و سفارش پرداخت
-- ============================================================
create table if not exists public.wallets (
    user_id uuid primary key references auth.users(id) on delete cascade,
    balance bigint not null default 0,
    updated_at timestamptz not null default now(),
    constraint wallet_balance_non_negative check (balance >= 0),
    constraint wallet_balance_cap check (balance <= 10000000)
);
create table if not exists public.wallet_tx (
    id bigserial primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    amount bigint not null,
    reason text,
    balance_after bigint,
    created_at timestamptz not null default now()
);
alter table public.wallet_tx add column if not exists operation_key uuid;
create unique index if not exists ux_wallet_tx_operation on public.wallet_tx(operation_key) where operation_key is not null;
create index if not exists idx_wallet_tx_user on public.wallet_tx(user_id, created_at desc);

alter table public.wallets enable row level security;
alter table public.wallet_tx enable row level security;
drop policy if exists p_wallet_own_read on public.wallets;
create policy p_wallet_own_read on public.wallets for select to authenticated using (user_id = auth.uid());
drop policy if exists p_wallet_tx_own on public.wallet_tx;
create policy p_wallet_tx_own on public.wallet_tx for select to authenticated using (user_id = auth.uid());

create table if not exists public.wallet_payment_orders (
    id bigserial primary key,
    operation_id uuid not null default gen_random_uuid() unique,
    user_id uuid not null references auth.users(id) on delete cascade,
    amount_toman bigint not null check (amount_toman >= 100000 and amount_toman <= 10000000 and amount_toman % 10000 = 0),
    amount_rial bigint not null check (amount_rial > 0),
    provider text not null check (provider in ('zarinpal', 'idpay', 'sandbox')),
    authority text unique,
    ref_id text unique,
    status text not null default 'pending' check (status in ('pending', 'requested', 'verifying', 'paid', 'failed', 'canceled')),
    error_code text,
    created_at timestamptz not null default now(),
    paid_at timestamptz
);
create index if not exists idx_wallet_payment_user on public.wallet_payment_orders(user_id, created_at desc);
create unique index if not exists ux_wallet_payment_one_open
    on public.wallet_payment_orders(user_id)
    where status in ('pending', 'requested', 'verifying');

alter table public.wallet_payment_orders enable row level security;
drop policy if exists p_wallet_payment_own_read on public.wallet_payment_orders;
create policy p_wallet_payment_own_read on public.wallet_payment_orders
for select to authenticated using (user_id = auth.uid());

create or replace function public.native_wallet_snapshot()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_balance bigint;
    v_transactions jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid;
    select coalesce(jsonb_agg(jsonb_build_object(
        'id', x.id,
        'amount', x.amount,
        'reason', x.reason,
        'balance_after', x.balance_after,
        'created_at', x.created_at
    ) order by x.created_at desc), '[]'::jsonb)
    into v_transactions
    from (
        select id, amount, reason, balance_after, created_at
        from public.wallet_tx
        where user_id = v_uid
        order by created_at desc
        limit 50
    ) x;
    return jsonb_build_object(
        'ok', true,
        'balance', coalesce(v_balance, 0),
        'currency', 'toman',
        'transactions', coalesce(v_transactions, '[]'::jsonb)
    );
end;
$$;
revoke all on function public.native_wallet_snapshot() from public, anon;
grant execute on function public.native_wallet_snapshot() to authenticated;

create or replace function public.native_create_wallet_payment_order(
    p_user uuid,
    p_amount_toman bigint,
    p_provider text
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_balance bigint;
    v_id bigint;
    v_operation uuid;
begin
    if p_user is null or not exists (
        select 1 from public.profiles where id = p_user and role = 'teacher'
    ) then return jsonb_build_object('error', 'حساب معلم معتبر نیست'); end if;
    if p_amount_toman < 100000 or p_amount_toman > 10000000 or p_amount_toman % 10000 <> 0 then
        return jsonb_build_object('error', 'مبلغ شارژ نامعتبر است');
    end if;
    if p_provider not in ('zarinpal', 'idpay', 'sandbox') then
        return jsonb_build_object('error', 'درگاه نامعتبر است');
    end if;

    insert into public.wallets(user_id, balance) values (p_user, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = p_user for update;
    if v_balance + p_amount_toman > 10000000 then
        return jsonb_build_object('error', 'موجودی پس از شارژ از سقف مجاز بیشتر می‌شود');
    end if;
    -- سفارش نیمه‌کاره ناشی از قطع شبکه برای همیشه حساب را قفل نمی‌کند.
    update public.wallet_payment_orders
    set status = 'failed', error_code = 'expired_open_order'
    where user_id = p_user
      and status in ('pending', 'requested', 'verifying')
      and created_at < now() - interval '30 minutes';

    if exists (
        select 1 from public.wallet_payment_orders
        where user_id = p_user and status in ('pending', 'requested', 'verifying')
    ) then return jsonb_build_object('error', 'یک سفارش پرداخت باز دارید؛ ابتدا همان را تکمیل یا لغو کنید'); end if;

    insert into public.wallet_payment_orders(user_id, amount_toman, amount_rial, provider)
    values (p_user, p_amount_toman, p_amount_toman * 10, p_provider)
    returning id, operation_id into v_id, v_operation;
    return jsonb_build_object('ok', true, 'id', v_id, 'operation_id', v_operation);
end;
$$;
revoke all on function public.native_create_wallet_payment_order(uuid,bigint,text) from public, anon, authenticated;
grant execute on function public.native_create_wallet_payment_order(uuid,bigint,text) to service_role;

create or replace function public.native_set_wallet_payment_authority(p_order bigint, p_authority text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_count integer;
begin
    if coalesce(btrim(p_authority), '') = '' or length(p_authority) > 200 then
        return jsonb_build_object('error', 'شناسه درگاه نامعتبر است');
    end if;
    update public.wallet_payment_orders
    set authority = btrim(p_authority), status = 'requested', error_code = null
    where id = p_order and status = 'pending';
    get diagnostics v_count = row_count;
    if v_count = 0 then return jsonb_build_object('error', 'سفارش در وضعیت قابل شروع نیست'); end if;
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_set_wallet_payment_authority(bigint,text) from public, anon, authenticated;
grant execute on function public.native_set_wallet_payment_authority(bigint,text) to service_role;

create or replace function public.native_fail_wallet_payment_order(p_order bigint, p_status text, p_code text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if p_status not in ('failed', 'canceled') then return jsonb_build_object('error', 'وضعیت نامعتبر است'); end if;
    update public.wallet_payment_orders
    set status = p_status, error_code = left(coalesce(p_code, ''), 80)
    where id = p_order and status in ('pending', 'requested', 'verifying');
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_fail_wallet_payment_order(bigint,text,text) from public, anon, authenticated;
grant execute on function public.native_fail_wallet_payment_order(bigint,text,text) to service_role;

create or replace function public.native_credit_wallet_payment(p_order bigint, p_ref text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_order public.wallet_payment_orders%rowtype;
    v_balance bigint;
begin
    select * into v_order
    from public.wallet_payment_orders
    where id = p_order
    for update;
    if not found then return jsonb_build_object('error', 'سفارش یافت نشد'); end if;
    if v_order.status = 'paid' then
        return jsonb_build_object('ok', true, 'already_paid', true, 'balance', (
            select balance from public.wallets where user_id = v_order.user_id
        ), 'ref_id', v_order.ref_id);
    end if;
    if v_order.status not in ('requested', 'verifying') then
        return jsonb_build_object('error', 'سفارش در وضعیت قابل تأیید نیست');
    end if;
    if coalesce(btrim(p_ref), '') = '' or length(p_ref) > 200 then
        return jsonb_build_object('error', 'شماره پیگیری معتبر نیست');
    end if;
    if exists (select 1 from public.wallet_payment_orders where ref_id = p_ref and id <> p_order) then
        return jsonb_build_object('error', 'شماره پیگیری قبلاً مصرف شده است');
    end if;

    insert into public.wallets(user_id, balance) values (v_order.user_id, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_order.user_id for update;
    if v_balance + v_order.amount_toman > 10000000 then
        return jsonb_build_object('error', 'اعتبارسنجی شد اما سقف کیف پول اجازه شارژ نمی‌دهد؛ نیازمند بررسی پشتیبانی');
    end if;

    update public.wallets
    set balance = balance + v_order.amount_toman, updated_at = now()
    where user_id = v_order.user_id
    returning balance into v_balance;

    insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
    values (v_order.user_id, v_order.amount_toman, 'payment:' || v_order.provider, v_balance, v_order.operation_id);

    update public.wallet_payment_orders
    set status = 'paid', ref_id = btrim(p_ref), paid_at = now(), error_code = null
    where id = p_order;

    return jsonb_build_object('ok', true, 'balance', v_balance, 'ref_id', btrim(p_ref));
end;
$$;
revoke all on function public.native_credit_wallet_payment(bigint,text) from public, anon, authenticated;
grant execute on function public.native_credit_wallet_payment(bigint,text) to service_role;

-- شارژ و بازپرداخت آزاد نسخه WebView برای پرداخت واقعی ناامن‌اند.
do $$
begin
    if to_regprocedure('public.wallet_topup(bigint)') is not null then
        execute 'revoke all on function public.wallet_topup(bigint) from public, anon, authenticated';
        execute 'grant execute on function public.wallet_topup(bigint) to service_role';
    end if;
    if to_regprocedure('public.wallet_refund(bigint,text)') is not null then
        execute 'revoke all on function public.wallet_refund(bigint,text) from public, anon, authenticated';
        execute 'grant execute on function public.wallet_refund(bigint,text) to service_role';
    end if;
end;
$$;

-- ============================================================
-- 5) ذخیره اتمیک آزمون + کسر هزینه، با idempotency
-- ============================================================
create table if not exists public.native_exam_operations (
    operation_id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    exam_id text not null,
    result jsonb not null,
    created_at timestamptz not null default now()
);
create index if not exists idx_native_exam_operations_user on public.native_exam_operations(user_id, created_at desc);
alter table public.native_exam_operations enable row level security;

create or replace function public.native_question_fingerprints(p_questions jsonb, p_keys jsonb)
returns table(question_key text, fingerprint jsonb)
language sql
immutable
set search_path = public, pg_temp
as $$
    select
        coalesce(nullif(q.item->>'id', ''), '@' || q.ord::text) as question_key,
        q.item || coalesce((
            select k.item - 'i'
            from jsonb_array_elements(coalesce(p_keys, '[]'::jsonb)) k(item)
            where coalesce((k.item->>'i')::integer, -1) = q.ord - 1
            limit 1
        ), '{}'::jsonb) as fingerprint
    from jsonb_array_elements(coalesce(p_questions, '[]'::jsonb)) with ordinality q(item, ord);
$$;
revoke all on function public.native_question_fingerprints(jsonb,jsonb) from public, anon, authenticated;

create or replace function public.native_save_exam_v1(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_operation uuid;
    v_exam_id text;
    v_code text;
    v_title text;
    v_subject text;
    v_questions jsonb;
    v_keys jsonb;
    v_mode text;
    v_old public.exams%rowtype;
    v_old_keys jsonb := '[]'::jsonb;
    v_create boolean;
    v_has_answers boolean := false;
    v_billable integer := 0;
    v_cost bigint := 0;
    v_balance bigint := 0;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id = v_uid and role = 'teacher') then
        return jsonb_build_object('error', 'فقط معلم دسترسی دارد');
    end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
        return jsonb_build_object('error', 'داده آزمون نامعتبر است');
    end if;
    if coalesce(p_payload->>'operation_id', '') !~* '^[0-9a-f-]{36}$' then
        return jsonb_build_object('error', 'شناسه عملیات نامعتبر است');
    end if;
    v_operation := (p_payload->>'operation_id')::uuid;

    select result into v_prior
    from public.native_exam_operations
    where operation_id = v_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = v_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    v_exam_id := btrim(coalesce(p_payload->>'id', ''));
    v_title := btrim(coalesce(p_payload->>'title', ''));
    v_subject := btrim(coalesce(p_payload->>'subject', ''));
    v_questions := p_payload->'questions';
    v_keys := p_payload->'answer_key';
    v_mode := coalesce(p_payload->>'audience', 'all');
    if v_exam_id = '' or length(v_exam_id) > 100 then return jsonb_build_object('error', 'شناسه آزمون نامعتبر است'); end if;
    if v_title = '' or length(v_title) > 250 then return jsonb_build_object('error', 'عنوان آزمون نامعتبر است'); end if;
    if length(v_subject) > 250 then return jsonb_build_object('error', 'نام درس بیش از حد بلند است'); end if;
    if jsonb_typeof(v_questions) <> 'array' or jsonb_array_length(v_questions) < 1 or jsonb_array_length(v_questions) > 500 then
        return jsonb_build_object('error', 'تعداد سؤال‌ها باید بین ۱ و ۵۰۰ باشد');
    end if;
    if jsonb_typeof(v_keys) <> 'array' then return jsonb_build_object('error', 'کلید پاسخ نامعتبر است'); end if;
    if v_mode not in ('all', 'classes', 'students') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;
    if v_mode = 'classes' and coalesce(jsonb_array_length(p_payload->'classes'), 0) = 0 then
        return jsonb_build_object('error', 'حداقل یک کلاس انتخاب کنید');
    end if;
    if v_mode = 'students' and coalesce(jsonb_array_length(p_payload->'students'), 0) = 0 then
        return jsonb_build_object('error', 'حداقل یک دانش‌آموز انتخاب کنید');
    end if;

    select * into v_old from public.exams where id = v_exam_id;
    v_create := not found;
    if not v_create and v_old.teacher_id <> v_uid then
        return jsonb_build_object('error', 'آزمون متعلق به این حساب نیست');
    end if;

    if v_create then
        v_code := upper(btrim(coalesce(p_payload->>'code', '')));
        if v_code !~ '^[A-Z0-9]{4,12}$' then return jsonb_build_object('error', 'کد آزمون نامعتبر است'); end if;
        if exists (select 1 from public.exams where upper(code) = v_code) then
            return jsonb_build_object('error', 'کد آزمون تکراری است؛ دوباره تلاش کنید');
        end if;
        v_billable := jsonb_array_length(v_questions);
    else
        v_code := v_old.code;
        select coalesce(answers, '[]'::jsonb) into v_old_keys
        from public.exam_keys where exam_id = v_exam_id;
        v_has_answers := exists (select 1 from public.answers where exam_id = v_exam_id);
        if v_has_answers then
            select count(*) into v_billable
            from public.native_question_fingerprints(v_questions, v_keys) n
            left join public.native_question_fingerprints(v_old.questions, v_old_keys) o using (question_key)
            where o.question_key is null or n.fingerprint is distinct from o.fingerprint;
        else
            select count(*) into v_billable
            from public.native_question_fingerprints(v_questions, v_keys) n
            left join public.native_question_fingerprints(v_old.questions, v_old_keys) o using (question_key)
            where o.question_key is null;
        end if;
    end if;
    v_cost := v_billable * 1000;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object(
            'error', 'موجودی کیف پول کافی نیست',
            'balance', v_balance,
            'required', v_cost
        );
    end if;

    if v_create then
        insert into public.exams(
            id, teacher_id, title, subject, duration, code, total_score, is_open,
            shuffle_q, shuffle_opt, neg_marking, audience, teacher_message,
            attempts_allowed, attempt_on_timeout, grade_policy, attempt_cooldown, questions
        ) values (
            v_exam_id, v_uid, v_title, v_subject,
            greatest(0, least(1440, coalesce((p_payload->>'duration')::integer, 0))),
            v_code,
            greatest(0, coalesce((p_payload->>'total_score')::double precision, 0)),
            false,
            coalesce((p_payload->>'shuffle_q')::boolean, false),
            coalesce((p_payload->>'shuffle_opt')::boolean, false),
            greatest(0, coalesce((p_payload->>'neg_marking')::numeric, 0)),
            v_mode,
            nullif(btrim(coalesce(p_payload->>'teacher_message', '')), ''),
            greatest(1, least(5, coalesce((p_payload->>'attempts_allowed')::integer, 1))),
            coalesce((p_payload->>'attempt_on_timeout')::boolean, false),
            case when p_payload->>'grade_policy' in ('last', 'best', 'all') then p_payload->>'grade_policy' else 'last' end,
            greatest(0, least(1440, coalesce((p_payload->>'attempt_cooldown')::integer, 0))),
            v_questions
        );
    else
        update public.exams
        set title = v_title,
            subject = v_subject,
            duration = greatest(0, least(1440, coalesce((p_payload->>'duration')::integer, 0))),
            total_score = greatest(0, coalesce((p_payload->>'total_score')::double precision, 0)),
            shuffle_q = coalesce((p_payload->>'shuffle_q')::boolean, false),
            shuffle_opt = coalesce((p_payload->>'shuffle_opt')::boolean, false),
            neg_marking = greatest(0, coalesce((p_payload->>'neg_marking')::numeric, 0)),
            audience = v_mode,
            teacher_message = nullif(btrim(coalesce(p_payload->>'teacher_message', '')), ''),
            attempts_allowed = greatest(1, least(5, coalesce((p_payload->>'attempts_allowed')::integer, 1))),
            attempt_on_timeout = coalesce((p_payload->>'attempt_on_timeout')::boolean, false),
            grade_policy = case when p_payload->>'grade_policy' in ('last', 'best', 'all') then p_payload->>'grade_policy' else 'last' end,
            attempt_cooldown = greatest(0, least(1440, coalesce((p_payload->>'attempt_cooldown')::integer, 0))),
            questions = v_questions
        where id = v_exam_id and teacher_id = v_uid;
    end if;

    insert into public.exam_keys(exam_id, answers)
    values (v_exam_id, v_keys)
    on conflict (exam_id) do update set answers = excluded.answers;

    delete from public.exam_audience_classes where exam_id = v_exam_id;
    delete from public.exam_audience_students where exam_id = v_exam_id;
    if v_mode = 'classes' then
        insert into public.exam_audience_classes(exam_id, class_id)
        select v_exam_id, c.id
        from public.classes c
        join (select distinct value::uuid as id from jsonb_array_elements_text(p_payload->'classes')) wanted on wanted.id = c.id
        where c.teacher_id = v_uid;
        if (select count(*) from public.exam_audience_classes where exam_id = v_exam_id) <>
           (select count(distinct value) from jsonb_array_elements_text(p_payload->'classes')) then
            raise exception 'کلاس مخاطب نامعتبر است';
        end if;
    elsif v_mode = 'students' then
        insert into public.exam_audience_students(exam_id, student_id)
        select v_exam_id, p.id
        from public.profiles p
        join (select distinct value::uuid as id from jsonb_array_elements_text(p_payload->'students')) wanted on wanted.id = p.id
        where p.teacher_id = v_uid and p.role = 'student';
        if (select count(*) from public.exam_audience_students where exam_id = v_exam_id) <>
           (select count(distinct value) from jsonb_array_elements_text(p_payload->'students')) then
            raise exception 'دانش‌آموز مخاطب نامعتبر است';
        end if;
    end if;

    if v_cost > 0 then
        update public.wallets
        set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid
        returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (
            v_uid,
            -v_cost,
            case when v_create then 'exam:create:' else 'exam:update:' end || v_exam_id,
            v_balance,
            v_operation
        );
    end if;

    v_result := jsonb_build_object(
        'ok', true,
        'id', v_exam_id,
        'code', v_code,
        'billed_questions', v_billable,
        'cost', v_cost,
        'balance', v_balance
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (v_operation, v_uid, v_exam_id, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_save_exam_v1(jsonb) from public, anon;
grant execute on function public.native_save_exam_v1(jsonb) to authenticated;

create or replace function public.native_duplicate_exam_v2(p_exam text, p_operation uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_source public.exams%rowtype;
    v_new_id text := gen_random_uuid()::text;
    v_code text;
    v_try integer := 0;
    v_count integer;
    v_cost bigint;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    select result into v_prior from public.native_exam_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;

    select * into v_source from public.exams
    where id = p_exam and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    v_count := jsonb_array_length(coalesce(v_source.questions, '[]'::jsonb));
    v_cost := v_count * 1000;

    insert into public.wallets(user_id, balance) values (v_uid, 0)
    on conflict (user_id) do nothing;
    select balance into v_balance from public.wallets where user_id = v_uid for update;
    if v_balance < v_cost then
        return jsonb_build_object('error', 'موجودی کیف پول کافی نیست', 'balance', v_balance, 'required', v_cost);
    end if;

    loop
        v_try := v_try + 1;
        v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
        exit when not exists (select 1 from public.exams where code = v_code);
        if v_try >= 20 then raise exception 'ساخت کد یکتا ناموفق بود'; end if;
    end loop;

    insert into public.exams(
        id, title, subject, questions, code, teacher, total_score, is_open,
        duration, teacher_login, teacher_id, shuffle_q, shuffle_opt, neg_marking,
        opens_at, closes_at, class_id, audience, teacher_message,
        attempts_allowed, attempt_on_timeout, grade_policy, attempt_cooldown
    ) values (
        v_new_id, coalesce(v_source.title, '') || ' (کپی)', v_source.subject,
        v_source.questions, v_code, v_source.teacher, v_source.total_score, false,
        v_source.duration, v_source.teacher_login, v_uid, v_source.shuffle_q,
        v_source.shuffle_opt, v_source.neg_marking, null, null, null, 'all',
        v_source.teacher_message, v_source.attempts_allowed, v_source.attempt_on_timeout,
        v_source.grade_policy, v_source.attempt_cooldown
    );
    insert into public.exam_keys(exam_id, answers)
    select v_new_id, answers from public.exam_keys where exam_id = p_exam;

    if v_cost > 0 then
        update public.wallets set balance = balance - v_cost, updated_at = now()
        where user_id = v_uid returning balance into v_balance;
        insert into public.wallet_tx(user_id, amount, reason, balance_after, operation_key)
        values (v_uid, -v_cost, 'exam:duplicate:' || v_new_id, v_balance, p_operation);
    end if;

    v_result := jsonb_build_object(
        'ok', true, 'id', v_new_id, 'code', v_code,
        'billed_questions', v_count, 'cost', v_cost, 'balance', v_balance
    );
    insert into public.native_exam_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, v_new_id, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_duplicate_exam_v2(text,uuid) from public, anon;
grant execute on function public.native_duplicate_exam_v2(text,uuid) to authenticated;

commit;

notify pgrst, 'reload schema';
