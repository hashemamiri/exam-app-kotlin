-- V336.0 — مدیریت چندمدرسه‌ای کلاس‌ها و معلم‌ها برای مدیر
-- هدف: ساخت کلاس مستقل در هر مدرسه (با معلم یا بدون معلم)، اختصاص بعدی معلم،
-- و کپی کلاس + دانش‌آموزان به مدرسهٔ دیگر بدون کپی آزمون/پاسخ.
-- این فایل باید یک‌بار در SQL Editor پروژهٔ اصلی (و آزمایشی جدا) اجرا شود.

begin;

-- کلاس می‌تواند موقتاً بدون معلم باشد و مدیر بعداً معلم عضو همان مدرسه را اختصاص دهد.
alter table public.classes alter column teacher_id drop not null;
create index if not exists idx_classes_school_unassigned_v336
    on public.classes(school_id) where teacher_id is null;

-- ------------------------------------------------------------
-- ۱) معلم‌های یک مدرسهٔ مشخص (عضویت مستقل در هر مدرسه)
-- ------------------------------------------------------------
create or replace function public.native_manager_school_teachers_v336(p_school uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_school is null then return jsonb_build_object('error', 'مدرسه را مشخص کنید'); end if;
    if not exists (
        select 1 from public.schools s
        where s.id = p_school
          and (s.created_by = v_uid or exists (
              select 1 from public.school_memberships m
              where m.school_id = s.id and m.user_id = v_uid
                and m.staff_role = 'manager' and m.status = 'active'
          ))
    ) then
        return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
    end if;

    return jsonb_build_object('ok', true, 'school_id', p_school, 'items', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', p.id,
            'full_name', coalesce(p.full_name, ''),
            'username', coalesce(p.username, ''),
            'email', coalesce(u.email, ''),
            'employee_code', coalesce(p.employee_code, ''),
            'phone', coalesce(p.phone, ''),
            'status', sm.status,
            'joined_at', sm.joined_at,
            'wallet_balance', coalesce(w.balance, 0)
        ) order by p.full_name, p.username)
        from public.school_memberships sm
        join public.profiles p on p.id = sm.user_id
        left join auth.users u on u.id = sm.user_id
        left join public.wallets w on w.user_id = sm.user_id
        where sm.school_id = p_school and sm.staff_role = 'teacher' and sm.status <> 'removed'
    ), '[]'::jsonb));
end;
$$;

create or replace function public.native_manager_set_teacher_active_v336(
    p_teacher uuid,
    p_school uuid,
    p_active boolean
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_teacher is null or p_school is null then return jsonb_build_object('error', 'معلم و مدرسه را مشخص کنید'); end if;
    if not exists (
        select 1 from public.schools s
        where s.id = p_school
          and (s.created_by = v_uid or exists (
              select 1 from public.school_memberships m
              where m.school_id = s.id and m.user_id = v_uid
                and m.staff_role = 'manager' and m.status = 'active'
          ))
    ) then
        return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
    end if;

    update public.school_memberships
    set status = case when coalesce(p_active, false) then 'active' else 'disabled' end
    where school_id = p_school and user_id = p_teacher
      and staff_role = 'teacher' and status <> 'removed';
    get diagnostics v_count = row_count;
    if v_count = 0 then return jsonb_build_object('error', 'معلم در این مدرسه پیدا نشد'); end if;

    insert into public.school_admin_audit_v37(school_id, actor_id, target_id, action, details)
    values (p_school, v_uid, p_teacher, 'teacher_membership_status_v336',
            jsonb_build_object('active', coalesce(p_active, false)));
    return jsonb_build_object('ok', true, 'active', coalesce(p_active, false));
end;
$$;

-- ------------------------------------------------------------
-- ۲) ساخت کلاس مستقل در مدرسه، با معلم یا بدون معلم
-- ------------------------------------------------------------
create or replace function public.native_manager_save_school_class_v336(
    p_school uuid,
    p_name text,
    p_grade text default '',
    p_field text default '',
    p_teacher uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_id uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_school is null then return jsonb_build_object('error', 'مدرسه را مشخص کنید'); end if;
    if not exists (
        select 1 from public.schools s
        where s.id = p_school
          and (s.created_by = v_uid or exists (
              select 1 from public.school_memberships m
              where m.school_id = s.id and m.user_id = v_uid
                and m.staff_role = 'manager' and m.status = 'active'
          ))
    ) then
        return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
    end if;
    if btrim(coalesce(p_name, '')) = '' or length(btrim(p_name)) > 120 then
        return jsonb_build_object('error', 'نام کلاس باید ۱ تا ۱۲۰ نویسه باشد');
    end if;
    if greatest(length(coalesce(p_grade, '')), length(coalesce(p_field, ''))) > 120 then
        return jsonb_build_object('error', 'پایه و رشته حداکثر ۱۲۰ نویسه است');
    end if;
    if p_teacher is not null and not exists (
        select 1 from public.school_memberships m
        where m.school_id = p_school and m.user_id = p_teacher
          and m.staff_role = 'teacher' and m.status = 'active'
    ) then
        return jsonb_build_object('error', 'معلم انتخابی عضو فعال همین مدرسه نیست');
    end if;

    insert into public.classes(teacher_id, school_id, created_by, shared_with_manager, name, grade, field_of_study)
    values (
        p_teacher,
        p_school,
        v_uid,
        true,
        btrim(p_name),
        nullif(btrim(coalesce(p_grade, '')), ''),
        nullif(btrim(coalesce(p_field, '')), '')
    )
    returning id into v_id;

    insert into public.school_admin_audit_v37(school_id, actor_id, action, details)
    values (p_school, v_uid, 'school_class_created_v336',
            jsonb_build_object('class_id', v_id, 'teacher_id', p_teacher));
    return jsonb_build_object('ok', true, 'id', v_id, 'school_id', p_school, 'teacher_id', p_teacher);
end;
$$;

-- ------------------------------------------------------------
-- ۳) کلاس‌های یک مدرسه برای مدیر (شامل کلاس بدون معلم)
-- ------------------------------------------------------------
create or replace function public.native_manager_school_classes_v336(p_school uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_school is null then return jsonb_build_object('error', 'مدرسه را مشخص کنید'); end if;
    if not exists (
        select 1 from public.schools s
        where s.id = p_school
          and (s.created_by = v_uid or exists (
              select 1 from public.school_memberships m
              where m.school_id = s.id and m.user_id = v_uid
                and m.staff_role = 'manager' and m.status = 'active'
          ))
    ) then
        return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
    end if;

    return jsonb_build_object('ok', true, 'school_id', p_school, 'items', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', c.id,
            'name', c.name,
            'grade', coalesce(c.grade, ''),
            'field_of_study', coalesce(c.field_of_study, ''),
            'boys', (select count(*) from public.class_members cm join public.profiles p on p.id = cm.student_id where cm.class_id = c.id and p.gender = 'male'),
            'girls', (select count(*) from public.class_members cm join public.profiles p on p.id = cm.student_id where cm.class_id = c.id and p.gender = 'female'),
            'total', (select count(*) from public.class_members cm where cm.class_id = c.id),
            'created_at', c.created_at,
            'teacher_id', c.teacher_id,
            'teacher_name', coalesce(t.full_name, ''),
            'manager_created', coalesce(c.created_by, c.teacher_id) = v_uid
        ) order by c.created_at desc nulls last, c.name)
        from public.classes c
        left join public.profiles t on t.id = c.teacher_id
        where c.school_id = p_school
          and (coalesce(c.shared_with_manager, false) or c.created_by = v_uid or c.teacher_id is null)
    ), '[]'::jsonb));
end;
$$;

-- ------------------------------------------------------------
-- ۴) اختصاص/تغییر معلم کلاس توسط مدیر (فقط معلم عضو همان مدرسه)
-- ------------------------------------------------------------
create or replace function public.native_manager_assign_class_teacher_v336(
    p_class uuid,
    p_teacher uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_school uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_class is null then return jsonb_build_object('error', 'کلاس را مشخص کنید'); end if;

    select c.school_id into v_school
    from public.classes c
    where c.id = p_class
      and c.school_id is not null
      and (c.created_by = v_uid or c.teacher_id is null or coalesce(c.shared_with_manager, false))
      and exists (
          select 1 from public.schools s
          where s.id = c.school_id
            and (s.created_by = v_uid or exists (
                select 1 from public.school_memberships m
                where m.school_id = s.id and m.user_id = v_uid
                  and m.staff_role = 'manager' and m.status = 'active'
            ))
      );
    if v_school is null then return jsonb_build_object('error', 'کلاس در مدارس شما پیدا نشد'); end if;

    if p_teacher is not null and not exists (
        select 1 from public.school_memberships m
        where m.school_id = v_school and m.user_id = p_teacher
          and m.staff_role = 'teacher' and m.status = 'active'
    ) then
        return jsonb_build_object('error', 'معلم انتخابی عضو فعال همین مدرسه نیست');
    end if;

    update public.classes set teacher_id = p_teacher where id = p_class;
    insert into public.school_admin_audit_v37(school_id, actor_id, action, details)
    values (v_school, v_uid, 'class_teacher_assigned_v336',
            jsonb_build_object('class_id', p_class, 'teacher_id', p_teacher));
    return jsonb_build_object('ok', true, 'class_id', p_class, 'teacher_id', p_teacher);
end;
$$;

-- ------------------------------------------------------------
-- ۵) کپی کلاس + دانش‌آموزان به مدرسهٔ دیگر (مستقل؛ بدون آزمون/پاسخ)
-- ------------------------------------------------------------
create or replace function public.native_manager_copy_class_to_school_v336(
    p_class uuid,
    p_target_school uuid,
    p_teacher uuid default null,
    p_name text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_source public.classes%rowtype;
    v_new_id uuid;
    v_copied integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_class is null or p_target_school is null then
        return jsonb_build_object('error', 'کلاس مبدأ و مدرسهٔ مقصد را مشخص کنید');
    end if;

    select c.* into v_source
    from public.classes c
    where c.id = p_class
      and c.school_id is not null
      and (coalesce(c.shared_with_manager, false) or c.created_by = v_uid or c.teacher_id is null)
      and exists (
          select 1 from public.schools s
          where s.id = c.school_id
            and (s.created_by = v_uid or exists (
                select 1 from public.school_memberships m
                where m.school_id = s.id and m.user_id = v_uid
                  and m.staff_role = 'manager' and m.status = 'active'
            ))
      );
    if v_source.id is null then return jsonb_build_object('error', 'کلاس مبدأ در مدارس شما پیدا نشد'); end if;
    if v_source.school_id = p_target_school then
        return jsonb_build_object('error', 'مدرسهٔ مقصد باید با مدرسهٔ مبدأ متفاوت باشد');
    end if;
    if not exists (
        select 1 from public.schools s
        where s.id = p_target_school
          and (s.created_by = v_uid or exists (
              select 1 from public.school_memberships m
              where m.school_id = s.id and m.user_id = v_uid
                and m.staff_role = 'manager' and m.status = 'active'
          ))
    ) then
        return jsonb_build_object('error', 'مدرسهٔ مقصد متعلق به شما نیست');
    end if;
    if p_teacher is not null and not exists (
        select 1 from public.school_memberships m
        where m.school_id = p_target_school and m.user_id = p_teacher
          and m.staff_role = 'teacher' and m.status = 'active'
    ) then
        return jsonb_build_object('error', 'معلم انتخابی عضو فعال مدرسهٔ مقصد نیست');
    end if;

    insert into public.classes(teacher_id, school_id, created_by, shared_with_manager, name, grade, field_of_study)
    values (
        p_teacher,
        p_target_school,
        v_uid,
        true,
        coalesce(nullif(btrim(p_name), ''), v_source.name),
        v_source.grade,
        v_source.field_of_study
    )
    returning id into v_new_id;

    insert into public.class_members(class_id, student_id)
    select v_new_id, cm.student_id
    from public.class_members cm
    where cm.class_id = p_class
    on conflict (class_id, student_id) do nothing;
    get diagnostics v_copied = row_count;

    insert into public.school_students(school_id, student_id, created_by, shared_with_manager)
    select p_target_school, cm.student_id, v_uid, true
    from public.class_members cm
    where cm.class_id = p_class
    on conflict (school_id, student_id) do nothing;

    insert into public.school_admin_audit_v37(school_id, actor_id, action, details)
    values (p_target_school, v_uid, 'class_copied_from_school_v336',
            jsonb_build_object('source_class_id', p_class, 'source_school_id', v_source.school_id,
                               'new_class_id', v_new_id, 'copied_students', v_copied, 'teacher_id', p_teacher));
    return jsonb_build_object('ok', true, 'id', v_new_id, 'school_id', p_target_school,
                              'copied_students', v_copied, 'teacher_id', p_teacher);
end;
$$;

-- ------------------------------------------------------------
-- ۶) افزودن/حذف دانش‌آموز در کلاس مدیریتی حتی وقتی کلاس هنوز معلم ندارد
-- ------------------------------------------------------------
create or replace function public.native_manager_set_class_student_v40c(
    p_class uuid,
    p_student uuid,
    p_add boolean default true
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_schools uuid[];
    v_teacher uuid;
    v_class_school uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;

    select array_agg(distinct s_id) into v_schools
    from (
        select m.school_id as s_id from public.school_memberships m
        where m.user_id = v_uid and m.staff_role = 'manager' and m.status = 'active'
        union
        select s.id as s_id from public.schools s where s.created_by = v_uid
    ) u;

    if coalesce(array_length(v_schools, 1), 0) = 0 then
        return jsonb_build_object('error', 'مدرسه یافت نشد');
    end if;
    if p_class is null or p_student is null then
        return jsonb_build_object('error', 'کلاس و دانش‌آموز را مشخص کنید');
    end if;

    select c.teacher_id,
           coalesce(c.school_id, (
               select m.school_id from public.school_memberships m
               where m.school_id = any(v_schools) and m.user_id = c.teacher_id
                 and m.staff_role = 'teacher' and m.status = 'active'
               order by m.joined_at asc limit 1
           ))
    into v_teacher, v_class_school
    from public.classes c
    where c.id = p_class
      and (c.shared_with_manager = true or coalesce(c.created_by, c.teacher_id) = v_uid)
      and (
          c.school_id = any(v_schools)
          or (c.school_id is null and exists (
              select 1 from public.school_memberships m
              where m.school_id = any(v_schools) and m.user_id = c.teacher_id
                and m.staff_role = 'teacher' and m.status = 'active'
          ))
      );

    if v_class_school is null then
        return jsonb_build_object('error', 'کلاس در مدرسه یافت نشد یا خصوصی است');
    end if;

    if not (
        exists (
            select 1 from public.school_students ss
            where ss.school_id = any(v_schools)
              and ss.student_id = p_student
              and (
                  coalesce(ss.shared_with_manager, false) = true
                  or ss.created_by = v_uid
                  or exists (
                      select 1 from public.school_memberships mm
                      where mm.user_id = ss.created_by and mm.school_id = ss.school_id
                        and mm.staff_role = 'manager' and mm.status <> 'removed'
                  )
              )
        )
        or exists (
            select 1 from public.profiles p
            where p.id = p_student and p.role = 'student' and p.teacher_id = v_uid
        )
        or exists (
            select 1 from public.class_members cm
            join public.classes c2 on c2.id = cm.class_id
            where cm.student_id = p_student
              and c2.school_id = any(v_schools)
              and (coalesce(c2.shared_with_manager, false) = true or coalesce(c2.created_by, c2.teacher_id) <> c2.teacher_id)
        )
    ) then
        return jsonb_build_object('error', 'کلاس یا دانش‌آموز در مدرسه یافت نشد');
    end if;

    if coalesce(p_add, true) then
        insert into public.school_students(school_id, student_id, created_by, shared_with_manager)
        values (v_class_school, p_student, v_uid, true)
        on conflict (school_id, student_id) do nothing;

        insert into public.class_members(class_id, student_id)
        values (p_class, p_student)
        on conflict (class_id, student_id) do nothing;

        if v_teacher is not null then
            insert into public.teacher_student_links(teacher_id, student_id, source_class_id)
            values (v_teacher, p_student, p_class)
            on conflict (teacher_id, student_id) do nothing;
        end if;
    else
        delete from public.class_members
        where class_id = p_class and student_id = p_student;
    end if;

    return jsonb_build_object('ok', true);
end;
$$;

-- ------------------------------------------------------------
-- دسترسی‌ها
-- ------------------------------------------------------------
revoke all on function public.native_manager_school_teachers_v336(uuid) from public, anon;
revoke all on function public.native_manager_set_teacher_active_v336(uuid, uuid, boolean) from public, anon;
revoke all on function public.native_manager_save_school_class_v336(uuid, text, text, text, uuid) from public, anon;
revoke all on function public.native_manager_school_classes_v336(uuid) from public, anon;
revoke all on function public.native_manager_assign_class_teacher_v336(uuid, uuid) from public, anon;
revoke all on function public.native_manager_copy_class_to_school_v336(uuid, uuid, uuid, text) from public, anon;
revoke all on function public.native_manager_set_class_student_v40c(uuid, uuid, boolean) from public, anon;

grant execute on function public.native_manager_school_teachers_v336(uuid) to authenticated;
grant execute on function public.native_manager_set_teacher_active_v336(uuid, uuid, boolean) to authenticated;
grant execute on function public.native_manager_save_school_class_v336(uuid, text, text, text, uuid) to authenticated;
grant execute on function public.native_manager_school_classes_v336(uuid) to authenticated;
grant execute on function public.native_manager_assign_class_teacher_v336(uuid, uuid) to authenticated;
grant execute on function public.native_manager_copy_class_to_school_v336(uuid, uuid, uuid, text) to authenticated;
grant execute on function public.native_manager_set_class_student_v40c(uuid, uuid, boolean) to authenticated;

commit;
