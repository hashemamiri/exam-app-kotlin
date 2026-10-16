-- V311.0 — رفع اشکالات تعامل سه پنل (مدیر، معلم و دانش‌آموز):
-- ۱) رفع خطای ۵۰۰ (21000: more than one row returned by a subquery) در native_manager_teacher_classes_v40c هنگام عضویت معلم در چند مدرسهٔ یک مدیر.
-- ۲) پشتیبانی از پارامتر اختیاری p_school در native_manager_save_teacher_class_v40c و native_manager_remove_teacher_v40b برای مدیر چندمدرسه‌ای.
-- ۳) رفع خطای «کلاس یا دانش‌آموز در مدرسه یافت نشد» در native_manager_set_class_student_v40c برای دانش‌آموزان ساخته‌شده توسط مدیر یا اعضای کلاس‌های اشتراکی، و تنظیم خودکار shared_with_manager=true در native_attach_created_student_v37 برای مدیر.
-- ۴) پشتیبانی از نقش مدیر در native_add_student_to_classes_v22 برای افزودن دانش‌آموز به کلاس‌های مدرسه از صفحهٔ دانش‌آموزان.
-- ۵) اجرای خودکار ویرایش/حذف دانش‌آموز پس از تأیید معلم در native_teacher_decide_manager_request_v41 و تکمیل فیلدهای پایه/رشته/نام پدر در native_save_student_extra_v28 برای درخواست‌های مدیر.
-- ۶) هم‌ترازسازی شمارش دانش‌آموزان در native_manager_school_summary_v36 با دانش‌آموزان قابل‌مشاهدهٔ مدیر و امکان حذف مدرسهٔ بدون کلاس/دانش‌آموز در native_manager_delete_school_v1.
-- ۷) بازنویسی exam_attendance و exam_attend_summary بر اساس مخاطبان واقعی آزمون (کلاس‌ها/دانش‌آموزان/مدارس) به‌همراه تمام شرکت‌کنندگان واقعی (از جمله دانش‌آموزان ساخته‌شده توسط مدیر).
-- ۸) پشتیبانی از نقش مدیر در تقویم (cal_month، cal_save_note و cal_unseen_v59).

begin;

-- ۱) اصلاح داده‌های قبلی: دانش‌آموزانی که توسط مدیر ساخته شده‌اند باید برای مدیر قابل‌مشاهده و قابل‌تخصیص به کلاس باشند.
update public.school_students ss
   set shared_with_manager = true
 where coalesce(ss.shared_with_manager, false) = false
   and (
       exists (select 1 from public.profiles p where p.id = ss.created_by and p.role = 'manager')
       or exists (select 1 from public.school_memberships m where m.school_id = ss.school_id and m.user_id = ss.created_by and coalesce(m.staff_role, m.role) = 'manager')
       or exists (select 1 from public.profiles sp join public.profiles mp on mp.id = sp.teacher_id where sp.id = ss.student_id and mp.role = 'manager')
   );

-- ۲) اتصال دانش‌آموز جدید به مدرسه: اگر سازنده مدیر است، shared_with_manager = true ثبت شود.
create or replace function public.native_attach_created_student_v37(
    p_actor uuid,
    p_student uuid,
    p_class uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_role text;
    v_school uuid;
    v_class_teacher uuid;
    v_is_mgr boolean := false;
begin
    if p_actor is null or p_student is null then
        return jsonb_build_object('error', 'ورودی ناقص است');
    end if;
    select role into v_role from public.profiles where id = p_actor;
    if v_role not in ('teacher', 'manager') then
        return jsonb_build_object('error', 'نقش مجاز نیست');
    end if;
    v_is_mgr := (v_role = 'manager');

    if p_class is not null then
        select school_id, teacher_id into v_school, v_class_teacher
        from public.classes where id = p_class;
    end if;
    if v_school is null then
        v_school := public.native_user_school_id_v36(p_actor);
    end if;

    if v_school is not null then
        insert into public.school_students(school_id, student_id, created_by, shared_with_manager)
        values (v_school, p_student, p_actor, v_is_mgr)
        on conflict (school_id, student_id) do update
            set shared_with_manager = (public.school_students.shared_with_manager or excluded.shared_with_manager);
    end if;

    if v_class_teacher is not null then
        insert into public.teacher_student_links(teacher_id, student_id, school_id, source)
        values (v_class_teacher, p_student, v_school, 'class')
        on conflict (teacher_id, student_id) do nothing;
    elsif v_role = 'teacher' then
        insert into public.teacher_student_links(teacher_id, student_id, school_id, source)
        values (p_actor, p_student, v_school, 'created')
        on conflict (teacher_id, student_id) do nothing;
    end if;

    return jsonb_build_object('ok', true, 'school_id', v_school, 'shared_with_manager', v_is_mgr);
end;
$$;
revoke all on function public.native_attach_created_student_v37(uuid, uuid, uuid) from public, anon, authenticated;
grant execute on function public.native_attach_created_student_v37(uuid, uuid, uuid) to service_role;

-- ۳) افزودن/حذف دانش‌آموز در کلاس توسط مدیر (شامل دانش‌آموزان ساخته‌شده توسط مدیر و اعضای کلاس‌های اشتراکی)
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
    v_schools := public.native_manager_school_ids_v62(v_uid);
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
                 and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
               order by m.created_at asc limit 1
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
                and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
          ))
      );

    if v_teacher is null or v_class_school is null then
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
                        and coalesce(mm.staff_role, mm.role) = 'manager' and mm.status <> 'removed'
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

        insert into public.teacher_student_links(teacher_id, student_id, school_id, source)
        values (v_teacher, p_student, v_class_school, 'class')
        on conflict (teacher_id, student_id) do nothing;
    else
        delete from public.class_members
        where class_id = p_class and student_id = p_student;
    end if;

    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_manager_set_class_student_v40c(uuid, uuid, boolean) from public, anon;
grant execute on function public.native_manager_set_class_student_v40c(uuid, uuid, boolean) to authenticated;

-- ۴) افزودن دانش‌آموز به چند کلاس (پشتیبانی از هر دو نقش معلم و مدیر)
create or replace function public.native_add_student_to_classes_v22(
    p_student uuid,
    p_classes jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_schools uuid[];
    v_added integer := 0;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_student is null or p_classes is null or jsonb_typeof(p_classes) <> 'array' then
        return jsonb_build_object('error', 'ورودی نامعتبر است');
    end if;
    select role into v_role from public.profiles where id = v_uid;

    if v_role = 'manager' then
        v_schools := public.native_manager_school_ids_v62(v_uid);
        if coalesce(array_length(v_schools, 1), 0) = 0 then
            return jsonb_build_object('error', 'مدرسه یافت نشد');
        end if;
        with target_classes as (
            select c.id, c.teacher_id, coalesce(c.school_id, v_schools[1]) as school_id
            from public.classes c
            join (select distinct value::uuid as id from jsonb_array_elements_text(p_classes) where value ~* '^[0-9a-f-]{36}$') w on w.id = c.id
            where (c.shared_with_manager = true or coalesce(c.created_by, c.teacher_id) = v_uid)
              and (
                  c.school_id = any(v_schools)
                  or (c.school_id is null and exists (
                      select 1 from public.school_memberships m
                      where m.school_id = any(v_schools) and m.user_id = c.teacher_id
                        and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
                  ))
              )
        ), ins_cm as (
            insert into public.class_members(class_id, student_id)
            select tc.id, p_student from target_classes tc
            on conflict (class_id, student_id) do nothing
            returning class_id
        ), ins_ss as (
            insert into public.school_students(school_id, student_id, created_by, shared_with_manager)
            select distinct tc.school_id, p_student, v_uid, true from target_classes tc where tc.school_id is not null
            on conflict (school_id, student_id) do nothing
        ), ins_tsl as (
            insert into public.teacher_student_links(teacher_id, student_id, school_id, source)
            select distinct tc.teacher_id, p_student, tc.school_id, 'class' from target_classes tc where tc.teacher_id is not null
            on conflict (teacher_id, student_id) do nothing
        )
        select count(*) into v_added from ins_cm;
        return jsonb_build_object('ok', true, 'added', v_added);
    end if;

    if not public.native_teacher_can_see_student_v40c(v_uid, p_student) then
        return jsonb_build_object('error', 'دانش‌آموز یافت نشد یا دسترسی ندارید');
    end if;

    with inserted as (
        insert into public.class_members(class_id, student_id)
        select c.id, p_student
        from public.classes c
        join (
            select distinct value::uuid as id
            from jsonb_array_elements_text(p_classes)
            where value ~* '^[0-9a-f-]{36}$'
        ) wanted on wanted.id = c.id
        where c.teacher_id = v_uid
        on conflict (class_id, student_id) do nothing
        returning 1
    )
    select count(*) into v_added from inserted;

    return jsonb_build_object('ok', true, 'added', v_added);
end;
$$;
revoke all on function public.native_add_student_to_classes_v22(uuid, jsonb) from public, anon;
grant execute on function public.native_add_student_to_classes_v22(uuid, jsonb) to authenticated;

-- ۵) فهرست کلاس‌های معلم برای مدیر (رفع خطای 21000 در چندمدرسه‌ای با limit 1)
create or replace function public.native_manager_teacher_classes_v40c(p_teacher uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_schools uuid[];
    v_teacher_name text;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_schools := public.native_manager_school_ids_v62(v_uid);
    if coalesce(array_length(v_schools, 1), 0) = 0 then
        return jsonb_build_object('error', 'مدرسه یافت نشد');
    end if;
    if p_teacher is null or not exists (
        select 1 from public.school_memberships m
        where m.school_id = any(v_schools) and m.user_id = p_teacher
          and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
    ) then
        return jsonb_build_object('error', 'معلم در این مدرسه فعال نیست');
    end if;

    select coalesce(p.full_name, p.display_name, p.username, '') into v_teacher_name
    from public.profiles p where p.id = p_teacher;

    return jsonb_build_object(
        'ok', true,
        'teacher_id', p_teacher,
        'teacher_name', coalesce(v_teacher_name, ''),
        'items', coalesce((
            select jsonb_agg(jsonb_build_object(
                'id', c.id,
                'name', c.name,
                'grade', coalesce(c.grade, ''),
                'field', coalesce(c.field_of_study, ''),
                'school_id', coalesce(c.school_id, (
                    select m.school_id from public.school_memberships m
                    where m.school_id = any(v_schools) and m.user_id = p_teacher
                      and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
                    order by m.created_at asc limit 1
                )),
                'school_name', coalesce((
                    select s.name from public.schools s
                    where s.id = coalesce(c.school_id, (
                        select m.school_id from public.school_memberships m
                        where m.school_id = any(v_schools) and m.user_id = p_teacher
                          and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
                        order by m.created_at asc limit 1
                    ))
                ), ''),
                'total', (select count(*) from public.class_members cm where cm.class_id = c.id),
                'manager_created', coalesce(c.created_by, c.teacher_id) = v_uid
            ) order by c.created_at desc)
            from public.classes c
            where c.teacher_id = p_teacher
              and (c.shared_with_manager = true or coalesce(c.created_by, c.teacher_id) = v_uid)
              and (
                  c.school_id = any(v_schools)
                  or (c.school_id is null and exists (
                      select 1 from public.school_memberships m
                      where m.school_id = any(v_schools) and m.user_id = p_teacher
                        and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
                  ))
              )
        ), '[]'::jsonb)
    );
end;
$$;
revoke all on function public.native_manager_teacher_classes_v40c(uuid) from public, anon;
grant execute on function public.native_manager_teacher_classes_v40c(uuid) to authenticated;

-- ۶) ساخت کلاس برای معلم توسط مدیر (پشتیبانی از p_school اختیاری در چندمدرسه‌ای)
drop function if exists public.native_manager_save_teacher_class_v40c(uuid, text, text, text);
create or replace function public.native_manager_save_teacher_class_v40c(
    p_teacher uuid,
    p_name text,
    p_grade text default null,
    p_field text default null,
    p_school uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_schools uuid[];
    v_target_school uuid;
    v_common_count integer;
    v_id uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_schools := public.native_manager_school_ids_v62(v_uid);
    if coalesce(array_length(v_schools, 1), 0) = 0 then
        return jsonb_build_object('error', 'مدرسه یافت نشد');
    end if;
    if p_teacher is null then
        return jsonb_build_object('error', 'معلم را انتخاب کنید');
    end if;

    if p_school is not null then
        if not (p_school = any(v_schools)) then
            return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
        end if;
        if not exists (
            select 1 from public.school_memberships m
            where m.school_id = p_school and m.user_id = p_teacher
              and coalesce(m.staff_role, m.role) = 'teacher' and m.status = 'active'
        ) then
            return jsonb_build_object('error', 'معلم در مدرسهٔ انتخابی عضو نیست');
        end if;
        v_target_school := p_school;
    else
        select count(distinct m.school_id), min(m.school_id::text)::uuid
        into v_common_count, v_target_school
        from public.school_memberships m
        where m.school_id = any(v_schools)
          and m.user_id = p_teacher
          and coalesce(m.staff_role, m.role) = 'teacher'
          and m.status = 'active';

        if coalesce(v_common_count, 0) = 0 then
            return jsonb_build_object('error', 'معلم در این مدرسه فعال نیست');
        end if;
        if v_common_count > 1 then
            return jsonb_build_object('error', 'این معلم در چند مدرسه عضو است؛ ابتدا مدرسهٔ مقصد را مشخص کنید');
        end if;
    end if;

    if btrim(coalesce(p_name, '')) = '' or length(btrim(p_name)) > 120 then
        return jsonb_build_object('error', 'نام کلاس باید ۱ تا ۱۲۰ نویسه باشد');
    end if;
    if greatest(length(coalesce(p_grade, '')), length(coalesce(p_field, ''))) > 120 then
        return jsonb_build_object('error', 'پایه و رشته حداکثر ۱۲۰ نویسه است');
    end if;

    insert into public.classes(teacher_id, school_id, created_by, shared_with_manager, name, grade, field_of_study)
    values (
        p_teacher,
        v_target_school,
        v_uid,
        true,
        btrim(p_name),
        nullif(btrim(coalesce(p_grade, '')), ''),
        nullif(btrim(coalesce(p_field, '')), '')
    )
    returning id into v_id;

    return jsonb_build_object('ok', true, 'id', v_id, 'school_id', v_target_school);
end;
$$;
revoke all on function public.native_manager_save_teacher_class_v40c(uuid, text, text, text, uuid) from public, anon;
grant execute on function public.native_manager_save_teacher_class_v40c(uuid, text, text, text, uuid) to authenticated;

-- ۷) حذف معلم از مدرسه (پشتیبانی از p_school اختیاری) و حذف مدرسهٔ بدون کلاس/دانش‌آموز
drop function if exists public.native_manager_remove_teacher_v40b(uuid);
create or replace function public.native_manager_remove_teacher_v40b(
    p_teacher uuid,
    p_school uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_schools uuid[];
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    v_schools := public.native_manager_school_ids_v62(v_uid);
    if coalesce(array_length(v_schools, 1), 0) = 0 then
        return jsonb_build_object('error', 'مدرسه یافت نشد');
    end if;
    if p_teacher is null then return jsonb_build_object('error', 'معلم نامعتبر است'); end if;
    if p_school is not null and not (p_school = any(v_schools)) then
        return jsonb_build_object('error', 'این مدرسه متعلق به شما نیست');
    end if;

    delete from public.school_memberships
    where user_id = p_teacher
      and coalesce(staff_role, role) = 'teacher'
      and (p_school is null or school_id = p_school)
      and school_id = any(v_schools);
    get diagnostics v_count = row_count;
    if v_count = 0 then
        return jsonb_build_object('error', 'معلم در این مدرسه یافت نشد');
    end if;
    return jsonb_build_object('ok', true, 'removed', v_count);
end;
$$;
revoke all on function public.native_manager_remove_teacher_v40b(uuid, uuid) from public, anon;
grant execute on function public.native_manager_remove_teacher_v40b(uuid, uuid) to authenticated;

create or replace function public.native_manager_delete_school_v1(p_school uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid := auth.uid(); v_classes int; v_exams int; v_students int; v_mine int;
begin
  if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
  if not exists (select 1 from public.schools s where s.id=p_school and (s.created_by=v_uid
       or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=v_uid and coalesce(m.staff_role, m.role)='manager' and m.status='active'))) then
    return jsonb_build_object('error','این مدرسه متعلق به شما نیست');
  end if;
  select count(*) into v_mine from public.schools s where s.created_by=v_uid
     or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=v_uid and coalesce(m.staff_role, m.role)='manager' and m.status='active');
  if v_mine <= 1 then return jsonb_build_object('error','آخرین مدرسهٔ شما حذف نمی‌شود؛ می‌توانید نام آن را ویرایش کنید'); end if;
  select count(*) into v_classes from public.classes where school_id=p_school;
  select count(*) into v_exams from public.exams where school_id=p_school;
  select count(*) into v_students from public.school_students where school_id=p_school;
  if v_classes+v_exams+v_students > 0 then
    return jsonb_build_object('error', format('این مدرسه خالی نیست (%s کلاس، %s آزمون، %s دانش‌آموز)؛ اول آن‌ها را منتقل یا حذف کنید', v_classes, v_exams, v_students));
  end if;
  delete from public.school_memberships where school_id=p_school;
  delete from public.schools where id=p_school;
  return jsonb_build_object('ok',true);
end $$;
revoke all on function public.native_manager_delete_school_v1(uuid) from public,anon;
grant execute on function public.native_manager_delete_school_v1(uuid) to authenticated;

-- ۸) هم‌ترازسازی شمارش دانش‌آموزان قابل‌مشاهدهٔ مدیر در native_manager_school_summary_v36
create or replace function public.native_manager_school_summary_v36()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and coalesce(staff_role,role)='manager' and status='active'
             union select id from public.schools where created_by=auth.uid()),
teachers as(select distinct sm.user_id from public.school_memberships sm where sm.school_id in(select school_id from mine) and coalesce(sm.staff_role,sm.role)='teacher' and sm.status='active'),
vis_students as(
    select ss.student_id
    from public.school_students ss join mine on mine.school_id=ss.school_id
    where ss.shared_with_manager or ss.created_by=auth.uid()
       or exists(select 1 from public.school_memberships mm where mm.user_id=ss.created_by and mm.school_id=ss.school_id and coalesce(mm.staff_role,mm.role)='manager' and mm.status<>'removed')
       or exists(select 1 from public.class_members cm2 join public.classes c2 on c2.id=cm2.class_id where cm2.student_id=ss.student_id and c2.school_id=ss.school_id and (c2.shared_with_manager or coalesce(c2.created_by,c2.teacher_id)<>c2.teacher_id))
    union
    select cm.student_id
    from public.class_members cm join public.classes c on c.id=cm.class_id join mine on mine.school_id=c.school_id
    where c.shared_with_manager or c.created_by=auth.uid()
),
examset as(select e.id,e.teacher_id,e.total_score from public.exams e where e.school_id in(select school_id from mine)),
answer_stats as(select count(*) answers,coalesce(avg(case when coalesce(e.total_score,0)>0 then coalesce(a.total_grade,0)*100.0/e.total_score end),0) average_percent from public.answers a join examset e on e.id=a.exam_id)
select case when not exists(select 1 from mine) then jsonb_build_object('error','مدرسه فعال پیدا نشد') else jsonb_build_object(
 'ok',true,
 'school_id',(select school_id from mine limit 1),
 'school_name',(select string_agg(s.name,'، ' order by s.name) from public.schools s where s.id in(select school_id from mine)),
 'school_count',(select count(*) from mine),
 'province',(select min(coalesce(s.province,'')) from public.schools s where s.id in(select school_id from mine)),
 'city',(select min(coalesce(s.city,'')) from public.schools s where s.id in(select school_id from mine)),
 'teachers',(select count(*) from teachers),
 'students',(select count(distinct v.student_id) from vis_students v join public.profiles p on p.id=v.student_id where p.role='student'),
 'classes',(select count(*) from public.classes c where c.school_id in(select school_id from mine) and (c.shared_with_manager or coalesce(c.created_by,c.teacher_id)=auth.uid())),
 'exams',(select count(*) from examset),
 'answers',(select answers from answer_stats),
 'average_percent',(select round(average_percent::numeric,1) from answer_stats),
 'distributed_toman',(select coalesce(sum(amount_toman),0) from public.manager_wallet_transfers_v38 x where x.school_id in(select school_id from mine)),
 'teacher_activity',(select coalesce(jsonb_agg(jsonb_build_object(
   'teacher_id',p.id,'name',p.full_name,
   'exams',(select count(*) from examset e where e.teacher_id=p.id),
   'classes',(select count(*) from public.classes c where c.teacher_id=p.id and c.school_id in(select school_id from mine)),
   'students',(select count(distinct x) from (select ss.student_id x from public.school_students ss where ss.created_by=p.id and ss.school_id in(select school_id from mine)
               union select cm.student_id from public.class_members cm join public.classes c on c.id=cm.class_id where c.teacher_id=p.id and c.school_id in(select school_id from mine)) u),
   'wallet_balance',coalesce((select balance from public.wallets w where w.user_id=p.id),0)
 ) order by p.full_name),'[]'::jsonb) from public.profiles p where p.id in(select user_id from teachers))
) end;
$$;
revoke all on function public.native_manager_school_summary_v36() from public,anon;
grant execute on function public.native_manager_school_summary_v36() to authenticated;

-- ۹) ذخیرهٔ مشخصات تکمیلی دانش‌آموز (و ثبت در درخواست تأیید معلم در صورت ویرایش توسط مدیر)
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
    v_role text;
    v_owner uuid;
    v_req_id uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if greatest(
        length(coalesce(p_father_name, '')),
        length(coalesce(p_grade, '')),
        length(coalesce(p_field, ''))
    ) > 120 then
        return jsonb_build_object('error', 'هر مقدار حداکثر ۱۲۰ نویسه است');
    end if;

    select role into v_role from public.profiles where id = v_uid;
    select teacher_id into v_owner from public.profiles where id = p_student and role = 'student';
    if v_owner is null then
        return jsonb_build_object('error', 'دانش‌آموز یافت نشد یا دسترسی ندارید');
    end if;

    if v_owner = v_uid then
        update public.profiles
        set father_name    = nullif(btrim(coalesce(p_father_name, '')), ''),
            grade          = nullif(btrim(coalesce(p_grade, '')), ''),
            field_of_study = nullif(btrim(coalesce(p_field, '')), '')
        where id = p_student and teacher_id = v_uid and role = 'student';
        return jsonb_build_object('ok', true);
    end if;

    if v_role = 'manager' then
        select id into v_req_id
        from public.manager_approval_requests
        where manager_id = v_uid
          and teacher_id = v_owner
          and target_type = 'student'
          and target_id = p_student
          and action = 'edit'
          and status = 'pending'
          and expires_at > now()
        order by created_at desc
        limit 1;

        if v_req_id is not null then
            update public.manager_approval_requests
            set payload = coalesce(payload, '{}'::jsonb) || jsonb_build_object(
                'father_name', btrim(coalesce(p_father_name, '')),
                'grade', btrim(coalesce(p_grade, '')),
                'field_of_study', btrim(coalesce(p_field, ''))
            )
            where id = v_req_id;
            return jsonb_build_object('ok', true, 'approval_required', true);
        end if;
    end if;

    return jsonb_build_object('error', 'دانش‌آموز یافت نشد یا دسترسی ندارید');
end;
$$;
revoke all on function public.native_save_student_extra_v28(uuid, text, text, text, text) from public, anon;
grant execute on function public.native_save_student_extra_v28(uuid, text, text, text, text) to authenticated;

-- ۱۰) درخواست‌های مدیر ↔ معلم: اجرای خودکار ویرایش/حذف دانش‌آموز به محض تأیید معلم
create or replace function public.native_teacher_manager_requests_v41() returns jsonb language plpgsql security definer set search_path=public as $$
begin
 update manager_approval_requests set status='expired' where teacher_id=auth.uid() and status='pending' and expires_at<=now();
 return jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object(
   'id',r.id,'target_id',r.target_id,'target_type',r.target_type,'action',r.action,'status',r.status,'expires_at',r.expires_at,'created_at',r.created_at,
   'manager_name',coalesce(p.full_name,''),
   'target_name',case when r.target_type='class' then (select c.name from classes c where c.id=r.target_id)
                      else (select s.full_name from profiles s where s.id=r.target_id) end,
   'payload',coalesce(r.payload,'{}'::jsonb)) order by r.created_at desc)
   from manager_approval_requests r left join profiles p on p.id=r.manager_id where r.teacher_id=auth.uid() and r.created_at>now()-interval '30 days'),'[]'::jsonb));
end $$;
revoke all on function public.native_teacher_manager_requests_v41() from public,anon;
grant execute on function public.native_teacher_manager_requests_v41() to authenticated;

create or replace function public.native_manager_my_requests_v1() returns jsonb language plpgsql security definer set search_path=public as $$
begin
 update manager_approval_requests set status='expired' where manager_id=auth.uid() and status='pending' and expires_at<=now();
 return jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object(
   'id',r.id,'target_id',r.target_id,'target_type',r.target_type,'action',r.action,'status',r.status,'expires_at',r.expires_at,'created_at',r.created_at,'decided_at',r.decided_at,
   'teacher_name',coalesce(t.full_name,''),
   'target_name',case when r.target_type='class' then coalesce((select c.name from classes c where c.id=r.target_id), nullif(r.payload->>'name',''), 'کلاس حذف‌شده')
                      else coalesce((select s.full_name from profiles s where s.id=r.target_id),'حساب حذف‌شده') end,
   'payload',coalesce(r.payload,'{}'::jsonb)) order by r.created_at desc)
   from manager_approval_requests r left join profiles t on t.id=r.teacher_id where r.manager_id=auth.uid() and r.created_at>now()-interval '30 days'),'[]'::jsonb));
end $$;
revoke all on function public.native_manager_my_requests_v1() from public,anon;
grant execute on function public.native_manager_my_requests_v1() to authenticated;

create or replace function public.native_teacher_decide_manager_request_v41(
    p_request uuid,
    p_approve boolean,
    p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    r public.manager_approval_requests;
    v_final_status text;
    v_fn text;
    v_ln text;
    v_full text;
    v_un text;
    v_gen text;
begin
    select * into r from public.manager_approval_requests
    where id = p_request and teacher_id = auth.uid()
    for update;
    if r.id is null then return jsonb_build_object('error', 'درخواست یافت نشد'); end if;
    if r.status <> 'pending' then return jsonb_build_object('error', 'این درخواست قبلاً تعیین تکلیف شده است'); end if;
    if r.expires_at <= now() then
        update public.manager_approval_requests set status = 'expired' where id = r.id;
        return jsonb_build_object('error', 'درخواست منقضی شده است');
    end if;

    v_final_status := case when p_approve then 'approved' else 'rejected' end;
    update public.manager_approval_requests
    set status = v_final_status,
        decided_at = now(),
        decision_note = nullif(left(coalesce(p_note, ''), 500), '')
    where id = r.id;

    if p_approve and r.target_type = 'class' then
        if r.action = 'edit' then
            update public.classes
            set name = coalesce(nullif(btrim(r.payload->>'name'), ''), name),
                grade = nullif(btrim(coalesce(r.payload->>'grade', '')), ''),
                field_of_study = nullif(btrim(coalesce(r.payload->>'field', '')), '')
            where id = r.target_id and teacher_id = auth.uid();
        else
            delete from public.classes where id = r.target_id and teacher_id = auth.uid();
        end if;
        v_final_status := 'executed';
        update public.manager_approval_requests set status = 'executed', executed_at = now() where id = r.id;
    elsif p_approve and r.target_type = 'student' then
        if r.action = 'edit' and coalesce(r.payload->>'kind', '') <> 'reset_password' then
            v_fn := nullif(btrim(coalesce(r.payload->>'first_name', '')), '');
            v_ln := nullif(btrim(coalesce(r.payload->>'last_name', '')), '');
            v_full := coalesce(
                nullif(btrim(coalesce(r.payload->>'full_name', '')), ''),
                nullif(btrim(concat_ws(' ', v_fn, v_ln)), '')
            );
            v_un := nullif(lower(btrim(coalesce(r.payload->>'username', ''))), '');
            v_gen := case when r.payload->>'gender' in ('male', 'female') then r.payload->>'gender' else null end;

            update public.profiles
            set first_name = coalesce(v_fn, first_name),
                last_name = coalesce(v_ln, last_name),
                full_name = coalesce(v_full, full_name),
                display_name = coalesce(v_full, display_name),
                username = coalesce(v_un, username),
                gender = coalesce(v_gen, gender),
                father_name = case when r.payload ? 'father_name' then nullif(btrim(coalesce(r.payload->>'father_name', '')), '') else father_name end,
                grade = case when r.payload ? 'grade' then nullif(btrim(coalesce(r.payload->>'grade', '')), '') else grade end,
                field_of_study = case when r.payload ? 'field_of_study' then nullif(btrim(coalesce(r.payload->>'field_of_study', '')), '') else field_of_study end
            where id = r.target_id and teacher_id = auth.uid() and role = 'student';

            if v_un is not null then
                begin
                    update auth.users
                    set email = v_un || '@student.exam.local',
                        updated_at = now()
                    where id = r.target_id;
                exception when others then null;
                end;
            end if;

            v_final_status := 'executed';
            update public.manager_approval_requests set status = 'executed', executed_at = now() where id = r.id;
        elsif r.action = 'delete' then
            begin
                delete from public.profiles where id = r.target_id and teacher_id = auth.uid() and role = 'student';
                delete from auth.users where id = r.target_id;
                v_final_status := 'executed';
                update public.manager_approval_requests set status = 'executed', executed_at = now() where id = r.id;
            exception when others then null;
            end;
        end if;
    end if;

    return jsonb_build_object(
        'ok', true,
        'status', v_final_status,
        'target_type', r.target_type,
        'target_id', r.target_id,
        'action', r.action
    );
end;
$$;
revoke all on function public.native_teacher_decide_manager_request_v41(uuid, boolean, text) from public, anon;
grant execute on function public.native_teacher_decide_manager_request_v41(uuid, boolean, text) to authenticated;

-- ۱۱) حضور و غیاب آزمون (exam_attendance و exam_attend_summary) بر اساس مخاطبان واقعی آزمون + شرکت‌کنندگان واقعی
do $drop_att$
declare r record;
begin
    for r in
        select p.oid::regprocedure as sig
        from pg_proc p join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public' and p.proname in ('exam_attendance', 'exam_attend_summary')
    loop
        execute format('drop function %s', r.sig);
    end loop;
end
$drop_att$;

create or replace function public.exam_attendance(p_exam text)
returns table(
    student_id uuid,
    full_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    status text,
    total_grade double precision,
    submitted_at timestamptz,
    expires_at timestamptz,
    minutes_left integer,
    attempts integer,
    attempts_allowed integer,
    abandoned integer
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam public.exams%rowtype;
begin
    if v_uid is null then return; end if;
    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid;
    if not found then return; end if;

    return query
    with aud as (
        select cm.student_id
        from public.exam_audience_classes ac
        join public.class_members cm on cm.class_id = ac.class_id
        where v_exam.audience = 'classes' and ac.exam_id = p_exam
        union
        select eas.student_id
        from public.exam_audience_students eas
        where v_exam.audience in ('students', 'schools') and eas.exam_id = p_exam
        union
        select ss.student_id
        from public.exam_audience_schools es
        join public.school_students ss on ss.school_id = es.school_id
        where v_exam.audience = 'schools' and es.exam_id = p_exam
        union
        select p.id as student_id
        from public.profiles p
        where coalesce(v_exam.audience, 'all') = 'all'
          and p.role = 'student'
          and (
              p.teacher_id = v_uid
              or exists (
                  select 1 from public.class_members cm
                  join public.classes c on c.id = cm.class_id
                  where cm.student_id = p.id and c.teacher_id = v_uid
              )
              or exists (
                  select 1 from public.teacher_student_links l
                  where l.student_id = p.id and l.teacher_id = v_uid
              )
          )
        union
        select a.student_id from public.answers a where a.exam_id = p_exam and a.student_id is not null
        union
        select ea.student_id from public.exam_attempts ea where ea.exam_id = p_exam and ea.student_id is not null
        union
        select es.student_id from public.exam_sessions es where es.exam_id = p_exam and es.student_id is not null
    ),
    ans as (
        select distinct on (a.student_id)
            a.student_id,
            a.total_grade::double precision as total_grade,
            a.submitted_at,
            coalesce(a.graded, false) as graded
        from public.answers a
        where a.exam_id = p_exam and a.student_id is not null
        order by a.student_id, a.submitted_at desc nulls last
    ),
    att as (
        select
            ea.student_id,
            count(*) filter (where ea.submitted_at is not null)::integer as submitted_count,
            count(*) filter (where coalesce(ea.abandoned, false))::integer as abandoned_count,
            max(ea.expires_at) filter (where ea.submitted_at is null and not coalesce(ea.abandoned, false)) as open_expires,
            bool_or(ea.submitted_at is null and not coalesce(ea.abandoned, false)) as has_open
        from public.exam_attempts ea
        where ea.exam_id = p_exam and ea.student_id is not null
        group by ea.student_id
    )
    select
        p.id as student_id,
        coalesce(p.full_name, p.display_name, p.username, '')::text as full_name,
        coalesce(p.first_name, '')::text as first_name,
        coalesce(p.last_name, '')::text as last_name,
        p.username::text as username,
        p.gender::text as gender,
        case
            when ans.student_id is not null then 'submitted'
            when coalesce(att.has_open, false) and (att.open_expires is null or att.open_expires > now()) then 'in_progress'
            when coalesce(att.has_open, false) and att.open_expires <= now() then 'expired'
            else 'absent'
        end::text as status,
        ans.total_grade,
        ans.submitted_at,
        att.open_expires as expires_at,
        case
            when coalesce(att.has_open, false) and att.open_expires is not null and att.open_expires > now()
                then greatest(0, ceil(extract(epoch from (att.open_expires - now())) / 60.0)::integer)
            else null
        end as minutes_left,
        greatest(coalesce(att.submitted_count, 0), case when ans.student_id is not null then 1 else 0 end)::integer as attempts,
        greatest(1, coalesce(v_exam.attempts_allowed, 1))::integer as attempts_allowed,
        coalesce(att.abandoned_count, 0)::integer as abandoned
    from (select distinct a.student_id from aud a where a.student_id is not null) u
    join public.profiles p on p.id = u.student_id
    left join ans on ans.student_id = p.id
    left join att on att.student_id = p.id
    order by
        case
            when ans.student_id is not null then 1
            when coalesce(att.has_open, false) then 2
            else 3
        end,
        coalesce(p.full_name, p.username, '');
end;
$$;
revoke all on function public.exam_attendance(text) from public, anon;
grant execute on function public.exam_attendance(text) to authenticated;

create or replace function public.exam_attend_summary(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_roster integer := 0;
    v_submitted integer := 0;
    v_started integer := 0;
    v_absent integer := 0;
    v_graded integer := 0;
begin
    select
        count(*)::integer,
        count(*) filter (where a.status = 'submitted')::integer,
        count(*) filter (where a.status in ('in_progress', 'started'))::integer,
        count(*) filter (where a.status in ('absent', 'not_started', 'expired'))::integer
    into v_roster, v_submitted, v_started, v_absent
    from public.exam_attendance(p_exam) a;

    select count(*)::integer into v_graded
    from public.answers ans
    join public.exams e on e.id = ans.exam_id
    where ans.exam_id = p_exam and e.teacher_id = auth.uid() and coalesce(ans.graded, false);

    return jsonb_build_object(
        'roster', v_roster,
        'submitted', v_submitted,
        'started', v_started,
        'absent', v_absent,
        'graded', v_graded
    );
end;
$$;
revoke all on function public.exam_attend_summary(text) from public, anon;
grant execute on function public.exam_attend_summary(text) to authenticated;

-- ۱۲) پشتیبانی از نقش مدیر در تقویم (cal_month، cal_save_note، cal_unseen_v59 و native_teacher_schools_v61)
create or replace function public.native_teacher_schools_v61()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select jsonb_build_object('ok',true,'items',coalesce((
    select jsonb_agg(jsonb_build_object(
        'id',s.id,'name',s.name,
        'province',coalesce(s.province,''),'city',coalesce(s.city,''),
        'classes',(select count(*) from public.classes c
                   where c.school_id=s.id and (
                       c.teacher_id=auth.uid()
                       or ((c.shared_with_manager or coalesce(c.created_by,c.teacher_id)=auth.uid())
                           and (s.created_by=auth.uid() or exists(
                               select 1 from public.school_memberships mm
                               where mm.school_id=s.id and mm.user_id=auth.uid()
                                 and coalesce(mm.staff_role,mm.role)='manager' and mm.status='active'
                           )))
                   ))
    ) order by s.name)
    from public.schools s
    where s.created_by=auth.uid()
       or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=auth.uid() and m.status='active')
),'[]'::jsonb));
$$;
revoke all on function public.native_teacher_schools_v61() from public, anon;
grant execute on function public.native_teacher_schools_v61() to authenticated;

create or replace function public.cal_save_note(
    p_date date,
    p_title text,
    p_body text default null,
    p_audience text default 'all',
    p_classes uuid[] default null,
    p_students uuid[] default null,
    p_id uuid default null,
    p_schools uuid[] default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_mgr_schools uuid[];
    v_id uuid;
    v_mode text := coalesce(p_audience, 'all');
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select role into v_role from public.profiles where id = v_uid;
    if v_role not in ('teacher', 'manager') then
        return jsonb_build_object('error', 'فقط معلم یا مدیر می‌تواند پیام ثبت کند');
    end if;
    if p_date is null or p_date < date '2021-03-21' or p_date > date '2122-03-21' then
        return jsonb_build_object('error', 'تاریخ خارج از بازه تقویم است');
    end if;
    if p_date < current_date then
        return jsonb_build_object('error', 'برای روزهای گذشته فقط حذف پیام ممکن است');
    end if;
    if coalesce(btrim(p_title), '') = '' then return jsonb_build_object('error', 'عنوان پیام لازم است'); end if;
    if length(btrim(p_title)) > 120 then return jsonb_build_object('error', 'عنوان حداکثر ۱۲۰ نویسه است'); end if;
    if length(coalesce(p_body, '')) > 2000 then return jsonb_build_object('error', 'توضیحات حداکثر ۲۰۰۰ نویسه است'); end if;
    if v_mode not in ('all', 'classes', 'students', 'schools') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;

    if v_role = 'manager' then
        v_mgr_schools := public.native_manager_school_ids_v62(v_uid);
    end if;

    if v_mode = 'classes' then
        if coalesce(cardinality(p_classes), 0) = 0 then return jsonb_build_object('error', 'حداقل یک کلاس انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_classes) x
            where not exists (
                select 1 from public.classes c
                where c.id = x and (
                    c.teacher_id = v_uid
                    or (v_role = 'manager' and c.school_id = any(v_mgr_schools))
                )
            )
        ) then return jsonb_build_object('error', 'یک کلاس انتخابی متعلق به شما نیست'); end if;
    elsif v_mode = 'students' then
        if coalesce(cardinality(p_students), 0) = 0 then return jsonb_build_object('error', 'حداقل یک دانش‌آموز انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_students) x
            where not exists (
                select 1 from public.profiles p
                where p.id = x and p.role = 'student'
                  and (
                      p.teacher_id = v_uid
                      or exists (select 1 from public.teacher_student_links l where l.teacher_id = v_uid and l.student_id = x)
                      or (v_role = 'manager' and exists (select 1 from public.school_students ss where ss.school_id = any(v_mgr_schools) and ss.student_id = x))
                  )
            )
        ) then return jsonb_build_object('error', 'یک دانش‌آموز انتخابی متعلق به شما نیست'); end if;
    elsif v_mode = 'schools' then
        if coalesce(cardinality(p_schools), 0) = 0 then return jsonb_build_object('error', 'حداقل یک مدرسه انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_schools) x
            where not (
                exists (
                    select 1 from public.school_memberships m
                    where m.school_id = x and m.user_id = v_uid and m.status = 'active'
                )
                or (v_role = 'manager' and x = any(v_mgr_schools))
            )
        ) then return jsonb_build_object('error', 'یک مدرسه انتخابی متعلق به شما نیست'); end if;
    end if;

    if p_id is null then
        insert into public.calendar_notes(teacher_id, on_date, title, body, audience)
        values (v_uid, p_date, btrim(p_title), nullif(btrim(coalesce(p_body, '')), ''), v_mode)
        returning id into v_id;
    else
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
        delete from public.calendar_note_schools where note_id = v_id;
    end if;

    if v_mode = 'classes' then
        insert into public.calendar_note_classes(note_id, class_id)
        select v_id, x from (select distinct unnest(p_classes) as x) s
        on conflict do nothing;
    elsif v_mode = 'students' then
        insert into public.calendar_note_students(note_id, student_id)
        select v_id, x from (select distinct unnest(p_students) as x) s
        on conflict do nothing;
    elsif v_mode = 'schools' then
        insert into public.calendar_note_schools(note_id, school_id)
        select v_id, x from (select distinct unnest(p_schools) as x) s
        on conflict do nothing;
    end if;

    return jsonb_build_object('ok', true, 'id', v_id);
end;
$$;
revoke all on function public.cal_save_note(date,text,text,text,uuid[],uuid[],uuid,uuid[]) from public, anon;
grant execute on function public.cal_save_note(date,text,text,text,uuid[],uuid[],uuid,uuid[]) to authenticated;

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
    if v_role in ('teacher', 'manager') then
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
              and (
                (n.audience = 'schools' and exists (
                    select 1 from public.calendar_note_schools ns
                    join public.school_students ss on ss.school_id = ns.school_id
                    where ns.note_id = n.id and ss.student_id = v_uid
                ))
                or (
                    (
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
                        or exists (
                            select 1 from public.school_students ss
                            join public.schools sc on sc.id = ss.school_id
                            where ss.student_id = v_uid
                              and (sc.created_by = n.teacher_id or exists (
                                  select 1 from public.school_memberships sm
                                  where sm.school_id = ss.school_id and sm.user_id = n.teacher_id
                                    and coalesce(sm.staff_role, sm.role) = 'manager' and sm.status = 'active'
                              ))
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
                )
              )
        ) t;
    end if;

    return jsonb_build_object('ok', true, 'notes', coalesce(v_notes, '[]'::jsonb));
end;
$$;
revoke all on function public.cal_month(date, date) from public, anon;
grant execute on function public.cal_month(date, date) to authenticated;

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
        (n.audience = 'schools' and exists (
            select 1 from public.calendar_note_schools ns
            join public.school_students ss on ss.school_id = ns.school_id
            where ns.note_id = n.id and ss.student_id = v_uid
        ))
        or (
            (
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
                or exists (
                    select 1 from public.school_students ss
                    join public.schools sc on sc.id = ss.school_id
                    where ss.student_id = v_uid
                      and (sc.created_by = n.teacher_id or exists (
                          select 1 from public.school_memberships sm
                          where sm.school_id = ss.school_id and sm.user_id = n.teacher_id
                            and coalesce(sm.staff_role, sm.role) = 'manager' and sm.status = 'active'
                      ))
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
            )
        )
      );

    return jsonb_build_object('ok', true, 'notes', coalesce(v_rows, '[]'::jsonb));
end;
$$;
revoke all on function public.cal_unseen_v59() from public, anon;
grant execute on function public.cal_unseen_v59() to authenticated;

commit;
