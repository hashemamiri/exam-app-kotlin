-- Only metadata, function fingerprints and constraint expressions. No scores, responses or identifiers.
BEGIN TRANSACTION READ ONLY;
SELECT current_setting('server_version') AS postgres_version;
SELECT p.proname AS function_name,
       pg_get_function_identity_arguments(p.oid) AS signature,
       md5(p.prosrc) AS body_fingerprint,
       strpos(p.prosrc, 'neg_marking') > 0 AS mentions_neg_marking,
       strpos(p.prosrc, 'shown_q') > 0 AS mentions_shown_q,
       strpos(p.prosrc, 'خارج از بازه') > 0 AS mentions_range_error,
       strpos(p.prosrc, 'نمره درس خارج از بازه') > 0 AS contains_reported_wording,
       strpos(p.prosrc, 'greatest(v_total,0)') > 0 AS has_submit_total_floor
FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
WHERE n.nspname='public' AND p.prokind='f'
AND (p.proname IN ('submit_answer','approve_auto_grades','native_auto_grade_question_v1',
 'exam_autograde_info','native_save_grade','native_save_grade_parts_v1','native_bulk_save_question_grades_v1',
 'native_finalize_bulk_grades_v1','native_my_answer_detail_v1','native_my_answer_detail_parts_v1')
 OR strpos(p.prosrc,'نمره درس خارج از بازه')>0)
ORDER BY p.proname,signature;
SELECT c.relname AS table_name,k.conname AS constraint_name,
       pg_get_constraintdef(k.oid) AS definition
FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid
JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND c.relname IN ('answers','exams','native_grade_progress')
AND k.contype='c'
ORDER BY c.relname,k.conname;
SELECT c.relname AS table_name,t.tgname AS trigger_name,p.proname AS function_name
FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_proc p ON p.oid=t.tgfoid
WHERE n.nspname='public' AND c.relname IN ('answers','exams','native_grade_progress')
AND NOT t.tgisinternal
ORDER BY c.relname,t.tgname;
ROLLBACK;
