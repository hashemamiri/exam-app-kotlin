-- V61.0 — «مدارس» برای معلم و مخاطب «مدارس» در پیام تقویم و آزمون.
-- ۱) لیست مدارس معلم + کلاس‌های معلم در هر مدرسه (دکمهٔ «مدارس» بخش کلاس‌ها).
-- ۲) مخاطب schools در پیام تقویم: انتخاب مدرسه = همهٔ دانش‌آموزان ثبت‌شده در
--    آن مدرسه (school_students)، حتی اگر عضو کلاسی نباشند.
-- ۳) مخاطب schools در آزمون: هنگام ذخیره به لیست دانش‌آموزان همان مدرسه‌ها
--    گسترش می‌یابد (exam_audience_students) تا مسیر ورود دانش‌آموز قدیمی
--    دست‌نخورده بماند؛ انتخاب مدرسه‌ها برای ویرایش بعدی در جدول جدا می‌ماند.

begin;

-- ------------------------------------------------------------
-- ۱) مدارس معلم
-- ------------------------------------------------------------
create or replace function public.native_teacher_schools_v61()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select jsonb_build_object('ok',true,'items',coalesce((
    select jsonb_agg(jsonb_build_object(
        'id',s.id,'name',s.name,
        'province',coalesce(s.province,''),'city',coalesce(s.city,''),
        'classes',(select count(*) from public.classes c
                   where c.school_id=s.id and c.teacher_id=auth.uid())
    ) order by s.name)
    from public.school_memberships m
    join public.schools s on s.id=m.school_id
    where m.user_id=auth.uid() and m.status='active'
),'[]'::jsonb));
$$;

create or replace function public.native_teacher_school_classes_v61(p_school uuid)
returns table(id uuid,name text,grade text,field_of_study text,boys integer,girls integer,total integer,created_at timestamptz)
language sql stable security definer set search_path=public,pg_temp as $$
    select c.id,c.name,c.grade,c.field_of_study,
           coalesce(count(*) filter (where p.gender='male'),0)::integer,
           coalesce(count(*) filter (where p.gender='female'),0)::integer,
           coalesce(count(p.id),0)::integer,c.created_at
    from public.classes c
    left join public.class_members m on m.class_id=c.id
    left join public.profiles p on p.id=m.student_id
    where c.teacher_id=auth.uid() and c.school_id=p_school
    group by c.id,c.name,c.grade,c.field_of_study,c.created_at
    order by c.created_at desc nulls last,c.name;
$$;

-- ------------------------------------------------------------
-- ۲) مخاطب schools در تقویم
-- ------------------------------------------------------------
alter table public.calendar_notes drop constraint if exists calendar_notes_audience_check;
alter table public.calendar_notes add constraint calendar_notes_audience_check
    check (audience in ('all','classes','students','schools'));

create table if not exists public.calendar_note_schools (
    note_id uuid not null references public.calendar_notes(id) on delete cascade,
    school_id uuid not null references public.schools(id) on delete cascade,
    primary key (note_id, school_id)
);
alter table public.calendar_note_schools enable row level security;
drop policy if exists p_cal_sch_own on public.calendar_note_schools;
create policy p_cal_sch_own on public.calendar_note_schools
for all to authenticated
using (exists (select 1 from public.calendar_notes n where n.id = note_id and n.teacher_id = auth.uid()))
with check (exists (select 1 from public.calendar_notes n where n.id = note_id and n.teacher_id = auth.uid()));

-- امضای تازه با p_schools؛ نسخهٔ ۷ پارامتری حذف می‌شود تا فراخوانی قدیمی مبهم نشود.
drop function if exists public.cal_save_note(date,text,text,text,uuid[],uuid[],uuid);
create function public.cal_save_note(
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
    if p_date < current_date then
        return jsonb_build_object('error', 'برای روزهای گذشته فقط حذف پیام ممکن است');
    end if;
    if coalesce(btrim(p_title), '') = '' then return jsonb_build_object('error', 'عنوان پیام لازم است'); end if;
    if length(btrim(p_title)) > 120 then return jsonb_build_object('error', 'عنوان حداکثر ۱۲۰ نویسه است'); end if;
    if length(coalesce(p_body, '')) > 2000 then return jsonb_build_object('error', 'توضیحات حداکثر ۲۰۰۰ نویسه است'); end if;
    if v_mode not in ('all', 'classes', 'students', 'schools') then return jsonb_build_object('error', 'نوع مخاطب نامعتبر است'); end if;

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
    elsif v_mode = 'schools' then
        if coalesce(cardinality(p_schools), 0) = 0 then return jsonb_build_object('error', 'حداقل یک مدرسه انتخاب کنید'); end if;
        if exists (
            select 1 from unnest(p_schools) x
            where not exists (
                select 1 from public.school_memberships m
                where m.school_id = x and m.user_id = v_uid and m.status = 'active'
            )
        ) then return jsonb_build_object('error', 'یک مدرسه انتخابی متعلق به این معلم نیست'); end if;
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

-- دید دانش‌آموز: mode=schools یعنی «ثبت‌شده در آن مدرسه» (school_students).
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
              and (
                -- V61.0: مخاطب مدرسه، مستقل از رابطهٔ مالک/لینک/کلاس دیده می‌شود
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

-- cal_day حالا مدرسه‌ها را هم برمی‌گرداند (برای ویرایش پیام).
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
        'students', (select coalesce(jsonb_agg(student_id), '[]'::jsonb) from public.calendar_note_students where note_id = v_note.id),
        'schools', (select coalesce(jsonb_agg(school_id), '[]'::jsonb) from public.calendar_note_schools where note_id = v_note.id)
    );
end;
$$;

-- ------------------------------------------------------------
-- ۳) مخاطب schools در آزمون (گسترش به دانش‌آموزان مدرسه هنگام ذخیره)
-- ------------------------------------------------------------
create table if not exists public.exam_audience_schools (
    exam_id text not null references public.exams(id) on delete cascade,
    school_id uuid not null references public.schools(id) on delete cascade,
    primary key (exam_id, school_id)
);
alter table public.exam_audience_schools enable row level security;
drop policy if exists p_exam_sch_own on public.exam_audience_schools;
create policy p_exam_sch_own on public.exam_audience_schools
for all to authenticated
using (exists (select 1 from public.exams e where e.id = exam_id and e.teacher_id = auth.uid()))
with check (exists (select 1 from public.exams e where e.id = exam_id and e.teacher_id = auth.uid()));

-- اعتبارسنجی دانش‌آموزِ مخاطب در native_save_exam_v1: علاوه بر مالکیت،
-- دانش‌آموز ثبت‌شده در مدرسه‌ای که معلم عضو فعال آن است هم پذیرفته می‌شود
-- (لازمهٔ گسترش school→students). فقط همین بند تغییر می‌کند؛ برای پرهیز از
-- کپی کامل تابع، بند insert دانش‌آموزان با تابع کمکی بازنویسی می‌شود.
create or replace function public.native_exam_school_students_v61(p_schools jsonb)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
    select coalesce(jsonb_agg(distinct ss.student_id::text), '[]'::jsonb)
    from jsonb_array_elements_text(coalesce(p_schools, '[]'::jsonb)) sc(id)
    join public.school_students ss on ss.school_id = sc.id::uuid
    join public.school_memberships m on m.school_id = ss.school_id
        and m.user_id = auth.uid() and m.status = 'active'
    join public.profiles p on p.id = ss.student_id and p.role = 'student';
$$;
revoke all on function public.native_exam_school_students_v61(jsonb) from public, anon;
grant execute on function public.native_exam_school_students_v61(jsonb) to authenticated;

-- exam_audience_students باید دانش‌آموزان مدرسه را هم بپذیرد؛ نسخهٔ v1 فقط
-- p.teacher_id = v_uid را قبول می‌کرد. این بازنویسی، همان منطق v1 است که
-- شرط عضویت مدرسه به آن اضافه شده (بند students).
create or replace function public.native_exam_audience_students_ok_v61(p_students jsonb)
returns boolean language sql stable security definer set search_path=public,pg_temp as $$
    select not exists (
        select 1 from jsonb_array_elements_text(coalesce(p_students,'[]'::jsonb)) w(id)
        where not exists (
            select 1 from public.profiles p
            where p.id = w.id::uuid and p.role = 'student'
              and (
                p.teacher_id = auth.uid()
                or exists (
                    select 1 from public.school_students ss
                    join public.school_memberships m on m.school_id = ss.school_id
                        and m.user_id = auth.uid() and m.status = 'active'
                    where ss.student_id = p.id
                )
                or exists (
                    select 1 from public.teacher_student_links l
                    where l.teacher_id = auth.uid() and l.student_id = p.id
                )
              )
        )
    );
$$;
revoke all on function public.native_exam_audience_students_ok_v61(jsonb) from public, anon;
grant execute on function public.native_exam_audience_students_ok_v61(jsonb) to authenticated;

-- v2 حالا mode=schools را به students گسترش می‌دهد و انتخاب مدرسه‌ها را برای
-- ویرایش بعدی نگه می‌دارد. درج مخاطبان دانش‌آموز پس از v1 دوباره با قواعد
-- V61 انجام می‌شود تا دانش‌آموزان مدرسه (متعلق به معلم دیگر) هم پذیرفته شوند.
create or replace function public.native_save_exam_v2(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_opens timestamptz;
    v_closes timestamptz;
    v_result jsonb;
    v_exam text;
    v_count integer;
    v_payload jsonb := p_payload;
    v_schools jsonb := '[]'::jsonb;
    v_students jsonb;
begin
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
        return jsonb_build_object('error','داده آزمون نامعتبر است');
    end if;
    begin
        v_opens := nullif(btrim(coalesce(p_payload->>'opens_at','')), '')::timestamptz;
        v_closes := nullif(btrim(coalesce(p_payload->>'closes_at','')), '')::timestamptz;
    exception when others then
        return jsonb_build_object('error','زمان بازشدن یا پایان معتبر نیست');
    end;
    if v_opens is not null and v_closes is not null and v_closes <= v_opens then
        return jsonb_build_object('error','مهلت پایان باید بعد از زمان بازشدن باشد');
    end if;

    -- V61.0 — مخاطب «مدارس»: گسترش به دانش‌آموزان ثبت‌شدهٔ همان مدرسه‌ها.
    if coalesce(p_payload->>'audience','all') = 'schools' then
        v_schools := coalesce(p_payload->'schools','[]'::jsonb);
        if coalesce(jsonb_array_length(v_schools),0) = 0 then
            return jsonb_build_object('error','حداقل یک مدرسه انتخاب کنید');
        end if;
        if exists (
            select 1 from jsonb_array_elements_text(v_schools) sc(id)
            where not exists (
                select 1 from public.school_memberships m
                where m.school_id = sc.id::uuid and m.user_id = v_uid and m.status = 'active'
            )
        ) then
            return jsonb_build_object('error','یک مدرسه انتخابی متعلق به این معلم نیست');
        end if;
        v_students := public.native_exam_school_students_v61(v_schools);
        if coalesce(jsonb_array_length(v_students),0) = 0 then
            return jsonb_build_object('error','مدرسه انتخابی دانش‌آموز ثبت‌شده ندارد');
        end if;
        v_payload := p_payload || jsonb_build_object('audience','students','students',v_students);
    end if;

    -- گسترش school ممکن است دانش‌آموزِ معلم دیگر را شامل شود که v1 رد می‌کند؛
    -- برای این حالت اعتبارسنجی/درج مخاطبان بعد از v1 با قواعد V61 انجام می‌شود.
    if coalesce(v_payload->>'audience','all') = 'students'
       and not public.native_exam_audience_students_ok_v61(v_payload->'students') then
        return jsonb_build_object('error','دانش‌آموز مخاطب نامعتبر است');
    end if;

    v_result := public.native_save_exam_v1(
        case when coalesce(v_payload->>'audience','all') = 'students'
             then v_payload || jsonb_build_object('audience','all') || jsonb_build_object('students','[]'::jsonb)
             else v_payload end
    );
    if v_result ? 'error' then return v_result; end if;
    v_exam := coalesce(v_result->>'id', v_payload->>'id');

    -- درج مخاطبان students با قواعد V61 (مالک یا هم‌مدرسه‌ای یا لینک‌شده).
    if coalesce(v_payload->>'audience','all') = 'students' then
        update public.exams set audience = 'students' where id = v_exam and teacher_id = v_uid;
        delete from public.exam_audience_students where exam_id = v_exam;
        insert into public.exam_audience_students(exam_id, student_id)
        select distinct v_exam, w.id::uuid
        from jsonb_array_elements_text(v_payload->'students') w(id);
    end if;
    delete from public.exam_audience_schools where exam_id = v_exam;
    if coalesce(jsonb_array_length(v_schools),0) > 0 then
        insert into public.exam_audience_schools(exam_id, school_id)
        select distinct v_exam, sc.id::uuid from jsonb_array_elements_text(v_schools) sc(id);
    end if;

    update public.exams
    set opens_at = v_opens,
        closes_at = v_closes
    where id = v_exam and teacher_id = v_uid;
    get diagnostics v_count = row_count;
    if v_count <> 1 then raise exception 'ذخیره زمان‌بندی آزمون کامل نشد'; end if;
    return v_result || jsonb_build_object('opens_at',v_opens,'closes_at',v_closes);
end;
$$;
revoke all on function public.native_save_exam_v2(jsonb) from public, anon;
grant execute on function public.native_save_exam_v2(jsonb) to authenticated;

-- خواندن مدرسه‌های آزمون برای ویرایش (کلاینت اگر خالی نبود mode=schools نشان می‌دهد).
create or replace function public.native_exam_audience_schools_v61(p_exam text)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
    select jsonb_build_object('ok',true,'schools',coalesce((
        select jsonb_agg(school_id::text)
        from public.exam_audience_schools s
        join public.exams e on e.id = s.exam_id and e.teacher_id = auth.uid()
        where s.exam_id = p_exam
    ),'[]'::jsonb));
$$;

revoke all on function public.native_teacher_schools_v61() from public, anon;
grant execute on function public.native_teacher_schools_v61() to authenticated;
revoke all on function public.native_teacher_school_classes_v61(uuid) from public, anon;
grant execute on function public.native_teacher_school_classes_v61(uuid) to authenticated;
revoke all on function public.native_exam_audience_schools_v61(text) from public, anon;
grant execute on function public.native_exam_audience_schools_v61(text) to authenticated;

commit;

-- سلامت‌سنجی پس از اجرا (هر دو باید true بدهند):
-- select position('schools' in pg_get_functiondef('public.cal_month(date,date)'::regprocedure)) > 0;
-- select position('native_exam_school_students_v61' in pg_get_functiondef('public.native_save_exam_v2(jsonb)'::regprocedure)) > 0;
