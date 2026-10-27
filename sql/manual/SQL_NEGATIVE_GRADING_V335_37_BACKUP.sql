-- READ ONLY. Export this result privately before applying the migration.
-- Save the full function definitions AND ACL/owner metadata; do not paste credentials.
begin transaction read only;
select p.proname as function_name, pg_get_function_identity_arguments(p.oid) as signature,
       pg_get_functiondef(p.oid) as function_definition,
       pg_get_userbyid(p.proowner) as owner_name, p.proacl as original_acl,
       md5(p.prosrc) as body_fingerprint
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='public' and p.prokind='f' and p.proname in (
 'native_save_grade','native_bulk_save_question_grades_v1','native_save_grade_parts_v1',
 'exam_autograde_info','submit_answer','approve_auto_grades','native_finalize_bulk_grades_v1',
 'native_grade_min_v335_37','native_question_shown_v335_37','native_auto_grade_with_penalty_v335_37')
order by p.proname,signature;
rollback;
