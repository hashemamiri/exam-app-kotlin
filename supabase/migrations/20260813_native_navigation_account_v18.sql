-- V18: پایه سربرگ، ویرایش امن بانک سؤال و backup profile v3
-- قابل اجرای مجدد، مالک‌محور و سازگار با safeupdate.
begin;

alter table public.profiles add column if not exists hdr_grade text;

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
            'hdr_grade', p.hdr_grade
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
        'hdr_grade', p.hdr_grade
    ) into v_result
    from public.profiles p
    where p.id = v_uid;
    return coalesce(v_result, jsonb_build_object('error', 'پروفایل ساخته نشد'));
end;
$$;

-- حذف امضای قدیمی برای جلوگیری از ambiguity در PostgREST.
drop function if exists public.native_save_profile(text,text,boolean,text,text,text,text);

create or replace function public.native_save_profile(
    p_display_name text,
    p_avatar_url text,
    p_avatar_public boolean,
    p_hdr_province text,
    p_hdr_city text,
    p_hdr_district text,
    p_hdr_school text,
    p_hdr_grade text
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
        length(coalesce(p_hdr_grade, ''))
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
        hdr_grade = case when v_role = 'teacher' then nullif(btrim(coalesce(p_hdr_grade, '')), '') else hdr_grade end
    where id = v_uid;

    return jsonb_build_object('ok', true, 'avatar_url', v_avatar);
end;
$$;

create or replace function public.native_bank_update_question_v1(
    p_id bigint,
    p_question jsonb,
    p_subject text,
    p_cats jsonb default '[]'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_text text;
    v_updated integer;
    v_categories jsonb;
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_question is null or jsonb_typeof(p_question) <> 'object' then
        return jsonb_build_object('error', 'سؤال نامعتبر است');
    end if;
    v_text := btrim(coalesce(p_question->>'text', ''));
    if v_text = '' or length(v_text) > 10000 then
        return jsonb_build_object('error', 'متن سؤال نامعتبر است');
    end if;
    if p_cats is null or jsonb_typeof(p_cats) <> 'array' then
        return jsonb_build_object('error', 'دسته‌ها نامعتبرند');
    end if;

    perform pg_advisory_xact_lock(hashtext(auth.uid()::text || ':bank-update:' || p_id::text));
    if exists(
        select 1 from public.question_bank q
        where q.teacher_id = auth.uid() and q.id <> p_id and q.question = p_question
    ) then
        return jsonb_build_object('error', 'این نسخه سؤال از قبل در بانک وجود دارد', 'duplicate', true);
    end if;

    update public.question_bank
    set question = p_question,
        subject = left(btrim(coalesce(p_subject, '')), 250)
    where id = p_id and teacher_id = auth.uid();
    get diagnostics v_updated = row_count;
    if v_updated <> 1 then
        return jsonb_build_object('error', 'سؤال یافت نشد یا دسترسی ندارید');
    end if;

    v_categories := public.native_bank_set_categories_v1(p_id, p_cats);
    if v_categories ? 'error' then raise exception '%', v_categories->>'error'; end if;
    return jsonb_build_object('ok', true, 'id', p_id);
end;
$$;

create or replace function public.native_export_backup_v2()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
    v_grade text;
begin
    v_result := public.native_export_backup_v1();
    if v_result ? 'error' then return v_result; end if;
    select hdr_grade into v_grade from public.profiles where id = v_uid;
    v_result := jsonb_set(v_result, '{_version}', '3'::jsonb, true);
    v_result := jsonb_set(
        v_result,
        '{profile,header,grade}',
        to_jsonb(coalesce(v_grade, '')),
        true
    );
    return v_result;
end;
$$;

create or replace function public.native_restore_backup_v2(
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
    v_grade text;
begin
    if coalesce(p_bundle->>'_version', '') !~ '^[0-9]+$'
       or (p_bundle->>'_version')::integer not in (1, 2, 3) then
        return jsonb_build_object('error', 'نسخه پشتیبان پشتیبانی نمی‌شود');
    end if;
    v_legacy := jsonb_set(p_bundle, '{_version}', '2'::jsonb, true);
    v_result := public.native_restore_backup_v1(p_operation, v_legacy, p_options);
    if v_result ? 'error' then return v_result; end if;

    if coalesce((p_options->>'header')::boolean, true)
       and jsonb_typeof(p_bundle->'profile'->'header') = 'object' then
        v_grade := left(btrim(coalesce(p_bundle->'profile'->'header'->>'grade', '')), 120);
        update public.profiles
        set hdr_grade = nullif(v_grade, '')
        where id = auth.uid();
    end if;
    return v_result || jsonb_build_object('profile_header_v3', true);
end;
$$;

revoke all on function public.native_my_profile() from public, anon;
revoke all on function public.native_ensure_profile_v1(text) from public, anon;
revoke all on function public.native_save_profile(text,text,boolean,text,text,text,text,text) from public, anon;
revoke all on function public.native_bank_update_question_v1(bigint,jsonb,text,jsonb) from public, anon;
revoke all on function public.native_export_backup_v2() from public, anon;
revoke all on function public.native_restore_backup_v2(uuid,jsonb,jsonb) from public, anon;

grant execute on function public.native_my_profile() to authenticated;
grant execute on function public.native_ensure_profile_v1(text) to authenticated;
grant execute on function public.native_save_profile(text,text,boolean,text,text,text,text,text) to authenticated;
grant execute on function public.native_bank_update_question_v1(bigint,jsonb,text,jsonb) to authenticated;
grant execute on function public.native_export_backup_v2() to authenticated;
grant execute on function public.native_restore_backup_v2(uuid,jsonb,jsonb) to authenticated;

commit;

select jsonb_build_object(
    'header_grade_ready', exists(
        select 1 from information_schema.columns
        where table_schema='public' and table_name='profiles' and column_name='hdr_grade'
    ),
    'profile_save_v18_ready', to_regprocedure('public.native_save_profile(text,text,boolean,text,text,text,text,text)') is not null,
    'bank_update_ready', to_regprocedure('public.native_bank_update_question_v1(bigint,jsonb,text,jsonb)') is not null,
    'backup_v2_ready', to_regprocedure('public.native_export_backup_v2()') is not null,
    'restore_v2_ready', to_regprocedure('public.native_restore_backup_v2(uuid,jsonb,jsonb)') is not null
) as v18_readiness;
