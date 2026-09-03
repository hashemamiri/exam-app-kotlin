-- V75.6 — بستن دسترسی «ناشناس» به توابع قدیمی (بند ۲.۳ گزارش امنیتی).
-- این ۳۰ تابع میراث دورهٔ WebView هستند، تعریفی در ریپو ندارند (بنابراین قابل ممیزی
-- نیستند) و در allowlist به anon هم مجاز شده‌اند. در نسخهٔ Native همهٔ جریان‌ها پس از
-- ورود انجام می‌شوند (هیچ ورود مهمان/ناشناسی در برنامه وجود ندارد)، بنابراین دادن
-- دسترسیِ «پیش از ورود» به این منطق فقط سطح حمله را بزرگ می‌کند و سودی ندارد.
--
-- استثنا: teacher_public_profile همچنان برای anon باز می‌ماند (ماهیتاً عمومی است).
--
-- برگشت: در صورت نیاز، همان نام‌ها را دوباره این‌طور مجاز کنید
--   grant execute on function public.<name>(<args>) to anon;

begin;

do $$
declare
    v_names text[] := array[
        'get_exam_for_student',
        'submit_answer',
        'my_answers',
        'my_grades',
        'my_classes',
        'create_class',
        'update_class',
        'delete_class',
        'class_roster_pick',
        'add_students_to_class',
        'remove_student_from_class',
        'my_students_for_pick',
        'save_student_extra',
        'set_student_active',
        'bank_add',
        'bank_list',
        'bank_del',
        'bank_move',
        'fb_add',
        'fb_list',
        'exam_attendance',
        'exam_attend_summary',
        'exam_live_status',
        'exam_autograde_info',
        'approve_auto_grades',
        'unapprove_grade',
        'reset_student_attempt',
        'extend_student_time',
        'get_exam_audience',
        'set_exam_audience'
    ];
    r record;
begin
    for r in
        select p.oid, p.proname, pg_get_function_identity_arguments(p.oid) as args
        from pg_proc p
        join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public'
          and p.proname = any(v_names)
    loop
        execute format('revoke execute on function public.%I(%s) from anon', r.proname, r.args);
    end loop;
end;
$$;

commit;

-- بررسیِ نتیجه (این بخش را جداگانه اجرا کنید):
-- select p.proname, p.prosecdef as security_definer,
--        array_to_string(array_agg(pr.rolname), ', ') as still_granted_to
-- from pg_proc p
-- join pg_namespace n on n.oid = p.pronamespace
-- left join pg_proc_grants pg on true
-- left join (
--     select a.oid, r.rolname
--     from pg_proc a, lateral aclexplode(a.proacl) x
--     join pg_roles r on r.oid = x.grantee
--     where x.privilege_type = 'EXECUTE'
-- ) pr on pr.oid = p.oid
-- where n.nspname = 'public' and p.proname = any(array['get_exam_for_student', 'submit_answer', 'my_answers', 'my_grades', 'my_classes', 'create_class', 'update_class', 'delete_class', 'class_roster_pick', 'add_students_to_class', 'remove_student_from_class', 'my_students_for_pick', 'save_student_extra', 'set_student_active', 'bank_add', 'bank_list', 'bank_del', 'bank_move', 'fb_add', 'fb_list', 'exam_attendance', 'exam_attend_summary', 'exam_live_status', 'exam_autograde_info', 'approve_auto_grades', 'unapprove_grade', 'reset_student_attempt', 'extend_student_time', 'get_exam_audience', 'set_exam_audience'])
-- group by p.proname, p.prosecdef
-- order by p.proname;
