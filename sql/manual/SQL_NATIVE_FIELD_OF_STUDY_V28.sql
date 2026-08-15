-- V28: رشته تحصیلی مستقل برای دانش‌آموز، کلاس و سربرگ رسمی
-- قابل اجرای مجدد، مالک‌محور، سازگار با safeupdate و بدون افشای رمز.
begin;

-- ============================================================
-- 1) ستون‌های واقعی رشته
-- ============================================================
alter table public.profiles add column if not exists field_of_study text;
alter table public.profiles add column if not exists hdr_field text;
alter table public.classes  add column if not exists field_of_study text;

-- ============================================================
-- 2) خواندن دانش‌آموزان همراه رشته
-- ============================================================
drop function if exists public.my_students();
create or replace function public.my_students()
returns table(
    id uuid,
    full_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    is_active boolean,
    class_names text,
    created_at timestamptz,
    father_name text,
    grade text,
    field_of_study text,
    avatar_url text
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true),
           coalesce((
               select string_agg(distinct c.name, '، ' order by c.name)
               from public.class_members m
               join public.classes c on c.id = m.class_id
               where m.student_id = p.id and c.teacher_id = auth.uid()
           ), ''),
           p.created_at, p.father_name, p.grade, p.field_of_study, p.avatar_url
    from public.profiles p
    where p.teacher_id = auth.uid() and p.role = 'student'
    order by p.full_name, p.username;
$$;

drop function if exists public.class_roster(uuid);
create or replace function public.class_roster(p_class uuid)
returns table(
    id uuid,
    full_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    is_active boolean,
    father_name text,
    grade text,
    field_of_study text,
    avatar_url text
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true), p.father_name, p.grade, p.field_of_study, p.avatar_url
    from public.class_members m
    join public.classes c on c.id = m.class_id
    join public.profiles p on p.id = m.student_id
    where m.class_id = p_class and c.teacher_id = auth.uid()
    order by p.full_name, p.username;
$$;

-- ============================================================
-- 3) ذخیره اطلاعات تکمیلی دانش‌آموز همراه رشته
-- ============================================================
create or replace function public.native_save_student_extra_v28(
    p_student uuid,
    p_username text,
    p_father_name text,
    p_grade text,
    p_field text
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
    if p_student is null or not exists (
        select 1 from public.profiles
        where id = p_student and teacher_id = v_uid and role = 'student'
    ) then
        return jsonb_build_object('error', 'دانش‌آموز یافت نشد یا دسترسی ندارید');
    end if;
    if greatest(
        length(coalesce(p_father_name, '')),
        length(coalesce(p_grade, '')),
        length(coalesce(p_field, ''))
    ) > 120 then
        return jsonb_build_object('error', 'هر مقدار حداکثر ۱۲۰ نویسه است');
    end if;

    update public.profiles
    set father_name    = nullif(btrim(coalesce(p_father_name, '')), ''),
        grade          = nullif(btrim(coalesce(p_grade, '')), ''),
        field_of_study = nullif(btrim(coalesce(p_field, '')), '')
    where id = p_student and teacher_id = v_uid and role = 'student';

    return jsonb_build_object('ok', true);
end;
$$;

-- ============================================================
-- 4) کلاس همراه رشته
-- ============================================================
create or replace function public.native_save_class_v28(
    p_class uuid,
    p_name text,
    p_grade text,
    p_field text
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_name text := btrim(coalesce(p_name, ''));
    v_id uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if v_name = '' then return jsonb_build_object('error', 'نام کلاس را وارد کنید'); end if;
    if greatest(length(v_name), length(coalesce(p_grade, '')), length(coalesce(p_field, ''))) > 120 then
        return jsonb_build_object('error', 'هر مقدار حداکثر ۱۲۰ نویسه است');
    end if;

    if p_class is null then
        insert into public.classes(teacher_id, name, grade, field_of_study)
        values (
            v_uid,
            v_name,
            nullif(btrim(coalesce(p_grade, '')), ''),
            nullif(btrim(coalesce(p_field, '')), '')
        )
        returning id into v_id;
        return jsonb_build_object('ok', true, 'id', v_id);
    end if;

    update public.classes
    set name = v_name,
        grade = nullif(btrim(coalesce(p_grade, '')), ''),
        field_of_study = nullif(btrim(coalesce(p_field, '')), '')
    where id = p_class and teacher_id = v_uid;
    if not found then return jsonb_build_object('error', 'کلاس یافت نشد یا دسترسی ندارید'); end if;

    return jsonb_build_object('ok', true, 'id', p_class);
end;
$$;

create or replace function public.native_my_classes_v28()
returns table(
    id uuid,
    name text,
    grade text,
    field_of_study text,
    boys integer,
    girls integer,
    total integer,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select c.id, c.name, c.grade, c.field_of_study,
           coalesce(count(*) filter (where p.gender = 'male'), 0)::integer,
           coalesce(count(*) filter (where p.gender = 'female'), 0)::integer,
           coalesce(count(p.id), 0)::integer,
           c.created_at
    from public.classes c
    left join public.class_members m on m.class_id = c.id
    left join public.profiles p on p.id = m.student_id
    where c.teacher_id = auth.uid()
    group by c.id, c.name, c.grade, c.field_of_study, c.created_at
    order by c.created_at desc nulls last, c.name;
$$;

-- ============================================================
-- 5) پروفایل و سربرگ رسمی همراه رشته
-- ============================================================
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
            'hdr_school', p.hdr_school,
            'hdr_grade', p.hdr_grade,
            'hdr_field', p.hdr_field
        )
        from public.profiles p
        where p.id = auth.uid()
    ), jsonb_build_object('error', 'پروفایل یافت نشد'));
$$;

create or replace function public.native_ensure_profile_v1(p_fallback_name text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_name text := left(coalesce(nullif(btrim(p_fallback_name), ''), 'کاربر'), 200);
    v_result jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    insert into public.profiles(id, full_name, display_name, role)
    values (v_uid, v_name, v_name, 'student')
    on conflict (id) do nothing;

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
        'hdr_school', p.hdr_school,
        'hdr_grade', p.hdr_grade,
        'hdr_field', p.hdr_field
    ) into v_result
    from public.profiles p
    where p.id = v_uid;
    return coalesce(v_result, jsonb_build_object('error', 'پروفایل ساخته نشد'));
end;
$$;

create or replace function public.native_save_profile_v28(
    p_display_name text,
    p_avatar_url text,
    p_avatar_public boolean,
    p_hdr_province text,
    p_hdr_city text,
    p_hdr_district text,
    p_hdr_school text,
    p_hdr_grade text,
    p_hdr_field text
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
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;

    select role, avatar_url into v_role, v_old_avatar
    from public.profiles
    where id = v_uid;
    if not found then return jsonb_build_object('error', 'پروفایل یافت نشد'); end if;

    if length(coalesce(p_display_name, '')) > 100 then
        return jsonb_build_object('error', 'نام نمایشی حداکثر ۱۰۰ نویسه است');
    end if;
    if greatest(
        length(coalesce(p_hdr_province, '')),
        length(coalesce(p_hdr_city, '')),
        length(coalesce(p_hdr_district, '')),
        length(coalesce(p_hdr_school, '')),
        length(coalesce(p_hdr_grade, '')),
        length(coalesce(p_hdr_field, ''))
    ) > 120 then
        return jsonb_build_object('error', 'هر بخش سربرگ حداکثر ۱۲۰ نویسه است');
    end if;
    if v_avatar is not null and length(v_avatar) > 2048 then
        return jsonb_build_object('error', 'نشانی آواتار بیش از حد بلند است');
    end if;
    if v_avatar is not null
       and v_avatar is distinct from v_old_avatar
       and position('/storage/v1/object/public/exam-images/avatars/' || v_uid::text || '/' in v_avatar) = 0 then
        return jsonb_build_object('error', 'مسیر آواتار با حساب فعلی مطابقت ندارد');
    end if;

    update public.profiles
    set display_name = nullif(btrim(coalesce(p_display_name, '')), ''),
        avatar_url = v_avatar,
        avatar_public = coalesce(p_avatar_public, true),
        hdr_province = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_province, '')), '') else hdr_province end,
        hdr_city = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_city, '')), '') else hdr_city end,
        hdr_district = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_district, '')), '') else hdr_district end,
        hdr_school = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_school, '')), '') else hdr_school end,
        hdr_grade = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_grade, '')), '') else hdr_grade end,
        hdr_field = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_field, '')), '') else hdr_field end
    where id = v_uid;

    return jsonb_build_object('ok', true, 'avatar_url', v_avatar);
end;
$$;

-- ============================================================
-- 6) پشتیبان نسخه ۴ همراه رشته سربرگ
-- ============================================================
create or replace function public.native_export_backup_v3()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
    v_field text;
begin
    v_result := public.native_export_backup_v2();
    if v_result ? 'error' then return v_result; end if;
    select hdr_field into v_field from public.profiles where id = v_uid;
    v_result := jsonb_set(v_result, '{_version}', '4'::jsonb, true);
    v_result := jsonb_set(
        v_result,
        '{profile,header,field}',
        to_jsonb(coalesce(v_field, '')),
        true
    );
    return v_result;
end;
$$;

create or replace function public.native_restore_backup_v3(
    p_operation uuid,
    p_bundle jsonb,
    p_options jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_result jsonb;
    v_legacy jsonb;
    v_field text;
begin
    if coalesce(p_bundle->>'_version', '') !~ '^[0-9]+$'
       or (p_bundle->>'_version')::integer not in (1, 2, 3, 4) then
        return jsonb_build_object('error', 'نسخه پشتیبان پشتیبانی نمی‌شود');
    end if;
    v_legacy := jsonb_set(p_bundle, '{_version}', '3'::jsonb, true);
    v_result := public.native_restore_backup_v2(p_operation, v_legacy, p_options);
    if v_result ? 'error' then return v_result; end if;

    if coalesce((p_options->>'header')::boolean, true)
       and jsonb_typeof(p_bundle->'profile'->'header') = 'object' then
        v_field := left(btrim(coalesce(p_bundle->'profile'->'header'->>'field', '')), 120);
        update public.profiles
        set hdr_field = nullif(v_field, '')
        where id = auth.uid();
    end if;
    return v_result || jsonb_build_object('profile_header_v4', true);
end;
$$;

-- ============================================================
-- 7) دسترسی حداقلی
-- ============================================================
revoke all on function public.my_students() from public, anon;
revoke all on function public.class_roster(uuid) from public, anon;
revoke all on function public.native_save_student_extra_v28(uuid,text,text,text,text) from public, anon;
revoke all on function public.native_save_class_v28(uuid,text,text,text) from public, anon;
revoke all on function public.native_my_classes_v28() from public, anon;
revoke all on function public.native_my_profile() from public, anon;
revoke all on function public.native_ensure_profile_v1(text) from public, anon;
revoke all on function public.native_save_profile_v28(text,text,boolean,text,text,text,text,text,text) from public, anon;
revoke all on function public.native_export_backup_v3() from public, anon;
revoke all on function public.native_restore_backup_v3(uuid,jsonb,jsonb) from public, anon;

grant execute on function public.my_students() to authenticated;
grant execute on function public.class_roster(uuid) to authenticated;
grant execute on function public.native_save_student_extra_v28(uuid,text,text,text,text) to authenticated;
grant execute on function public.native_save_class_v28(uuid,text,text,text) to authenticated;
grant execute on function public.native_my_classes_v28() to authenticated;
grant execute on function public.native_my_profile() to authenticated;
grant execute on function public.native_ensure_profile_v1(text) to authenticated;
grant execute on function public.native_save_profile_v28(text,text,boolean,text,text,text,text,text,text) to authenticated;
grant execute on function public.native_export_backup_v3() to authenticated;
grant execute on function public.native_restore_backup_v3(uuid,jsonb,jsonb) to authenticated;

commit;

select jsonb_build_object(
    'student_field_ready', exists(
        select 1 from information_schema.columns
        where table_schema='public' and table_name='profiles' and column_name='field_of_study'
    ),
    'class_field_ready', exists(
        select 1 from information_schema.columns
        where table_schema='public' and table_name='classes' and column_name='field_of_study'
    ),
    'header_field_ready', exists(
        select 1 from information_schema.columns
        where table_schema='public' and table_name='profiles' and column_name='hdr_field'
    ),
    'student_extra_ready',
        to_regprocedure('public.native_save_student_extra_v28(uuid,text,text,text,text)') is not null,
    'class_save_ready',
        to_regprocedure('public.native_save_class_v28(uuid,text,text,text)') is not null,
    'class_list_ready',
        to_regprocedure('public.native_my_classes_v28()') is not null,
    'profile_save_ready',
        to_regprocedure('public.native_save_profile_v28(text,text,boolean,text,text,text,text,text,text)') is not null,
    'backup_v4_ready',
        to_regprocedure('public.native_export_backup_v3()') is not null
) as v28_readiness;
