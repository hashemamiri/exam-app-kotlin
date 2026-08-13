-- V18 structural/security regression. Run after migration in the same test database.
begin;

do $$
begin
    if not exists (
        select 1 from information_schema.columns
        where table_schema='public' and table_name='profiles' and column_name='hdr_grade'
    ) then raise exception 'V18_TEST_FAIL: hdr_grade missing'; end if;

    if to_regprocedure('public.native_save_profile(text,text,boolean,text,text,text,text,text)') is null then
        raise exception 'V18_TEST_FAIL: profile save v18 missing';
    end if;
    if to_regprocedure('public.native_save_profile(text,text,boolean,text,text,text,text)') is not null then
        raise exception 'V18_TEST_FAIL: legacy profile overload remains';
    end if;
    if to_regprocedure('public.native_bank_update_question_v1(bigint,jsonb,text,jsonb)') is null then
        raise exception 'V18_TEST_FAIL: bank update missing';
    end if;
    if to_regprocedure('public.native_export_backup_v2()') is null
       or to_regprocedure('public.native_restore_backup_v2(uuid,jsonb,jsonb)') is null then
        raise exception 'V18_TEST_FAIL: backup v2 missing';
    end if;
end $$;

select
    has_function_privilege('authenticated', 'public.native_save_profile(text,text,boolean,text,text,text,text,text)', 'execute') as profile_authenticated,
    not has_function_privilege('anon', 'public.native_save_profile(text,text,boolean,text,text,text,text,text)', 'execute') as profile_anon_denied,
    has_function_privilege('authenticated', 'public.native_bank_update_question_v1(bigint,jsonb,text,jsonb)', 'execute') as bank_authenticated,
    not has_function_privilege('anon', 'public.native_bank_update_question_v1(bigint,jsonb,text,jsonb)', 'execute') as bank_anon_denied;

rollback;
