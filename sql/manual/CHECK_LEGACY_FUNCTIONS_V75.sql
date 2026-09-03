-- V75.6 — ابزارِ ممیزیِ توابع قدیمی (فقط خواندن؛ هیچ تغییری نمی‌دهد).
-- خروجی هر کوئری را در SQL Editor اجرا و در ریپو (docs/fa/) ذخیره کنید تا
-- منطقِ حساسِ خارج از کنترل نسخه بالاخره قابل بررسی شود.

-- ۱) تعریف کاملِ ۳۰ تابع قدیمی (برای بازبینی و انتقال تدریجی به ریپو)
select p.proname, pg_get_functiondef(p.oid) as definition
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname in (
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
  )
order by p.proname;

-- ۲) کدام توابعِ public هنوز به anon مجازند و کدام security definer هستند؟
select p.proname,
       p.prosecdef as security_definer,
       coalesce(array_to_string(array_agg(distinct r.rolname), ', '), '-') as execute_granted_to
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
left join lateral aclexplode(p.proacl) x on true
left join pg_roles r on r.oid = x.grantee and x.privilege_type = 'EXECUTE'
where n.nspname = 'public'
  and p.proname like 'native%' is false
group by p.proname, p.prosecdef
order by p.proname;

-- ۳) اندازه و وابستگی‌ها: کدام تابع‌های قدیمی هنوز از کدِ Native صدا زده می‌شوند؟
-- (خروجی این کوئری نشان می‌دهد کدام‌ها واقعاً زنده‌اند و کدام‌ها قابل حذف‌اند.)
select routine_name, routine_type, security_type
from information_schema.routines
where routine_schema = 'public'
  and routine_name in ('get_exam_for_student','submit_answer','my_grades','my_classes',
                       'exam_attendance','teacher_public_profile','fb_add')
order by routine_name;
