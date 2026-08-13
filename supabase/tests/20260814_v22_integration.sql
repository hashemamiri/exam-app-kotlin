-- V22 structural/grant regression
begin;

do $$
begin
    if to_regprocedure('public.native_add_student_to_classes_v22(uuid,jsonb)') is null then
        raise exception 'V22_TEST_FAIL: multi-class RPC missing';
    end if;
end $$;

select
    has_function_privilege('authenticated', 'public.native_add_student_to_classes_v22(uuid,jsonb)', 'execute') as authenticated_allowed,
    not has_function_privilege('anon', 'public.native_add_student_to_classes_v22(uuid,jsonb)', 'execute') as anon_denied;

rollback;
