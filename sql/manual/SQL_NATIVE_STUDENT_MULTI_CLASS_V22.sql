-- V22: افزودن اتمیک یک دانش‌آموز به چند کلاس متعلق به معلم
begin;

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
    v_requested integer;
    v_added integer;
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_student is null or not exists (
        select 1 from public.profiles
        where id = p_student and teacher_id = auth.uid() and role = 'student'
    ) then return jsonb_build_object('error', 'دانش‌آموز یافت نشد یا دسترسی ندارید'); end if;
    if p_classes is null or jsonb_typeof(p_classes) <> 'array' then
        return jsonb_build_object('error', 'فهرست کلاس‌ها نامعتبر است');
    end if;

    select count(distinct value)::integer into v_requested
    from jsonb_array_elements_text(p_classes);
    if v_requested < 1 or v_requested > 100 then
        return jsonb_build_object('error', 'یک تا صد کلاس انتخاب کنید');
    end if;
    if exists (
        select 1 from jsonb_array_elements_text(p_classes) x
        where x.value !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
    ) then return jsonb_build_object('error', 'شناسه کلاس نامعتبر است'); end if;
    if exists (
        select 1
        from jsonb_array_elements_text(p_classes) x
        where not exists (
            select 1 from public.classes c
            where c.id = x.value::uuid and c.teacher_id = auth.uid()
        )
    ) then return jsonb_build_object('error', 'یک یا چند کلاس نامعتبر است'); end if;

    insert into public.class_members(class_id, student_id)
    select distinct value::uuid, p_student
    from jsonb_array_elements_text(p_classes)
    on conflict do nothing;
    get diagnostics v_added = row_count;

    return jsonb_build_object('ok', true, 'added', v_added, 'requested', v_requested);
end;
$$;

revoke all on function public.native_add_student_to_classes_v22(uuid,jsonb) from public, anon;
grant execute on function public.native_add_student_to_classes_v22(uuid,jsonb) to authenticated;

commit;

select jsonb_build_object(
    'student_multi_class_ready',
    to_regprocedure('public.native_add_student_to_classes_v22(uuid,jsonb)') is not null
) as v22_readiness;
