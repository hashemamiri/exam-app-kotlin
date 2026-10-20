-- ═══════════════════════════════════════════════════════════════════════════════
-- V324 — گرنت‌های صریح «Data API» (پاسخ به تغییر ۳۰ اکتبر Supabase)
--
-- از ۳۰ اکتبر ۲۰۲۵/۲۰۲۶ (طبق ایمیل Supabase) جدول‌های *تازه* در schema عمومی دیگر
-- به‌صورت خودکار به anon / authenticated / service_role دسترسی نمی‌گیرند؛
-- جدول‌های موجود دست‌نخورده می‌مانند. پیامدهای عملی برای این پروژه:
--   • هر مهاجرت بعدی که «create table» می‌سازد، بدون GRANT صریح، از PostgREST و
--     توابع لبه (که با service_role کار می‌کنند) «permission denied» می‌گیرد.
--   • `supabase db reset`، شاخه‌های پیش‌نمایش و پروژهٔ تازه: همهٔ ۴۰ جدول بدون گرنت می‌مانند.
--
-- این فایل دو کار می‌کند:
--   ۱) پیش‌فرض‌های آینده را برمی‌گرداند (ALTER DEFAULT PRIVILEGES) تا جدول‌های تازه
--      دقیقاً مثل قبل در دسترس باشند.
--   ۲) برای هر ۴۰ جدول موجود، گرنت را *صریح* می‌نویسد (اجرای مکرر بی‌خطر است).
--
-- اجرا: SQL Editor پروژه (به‌عنوان postgres). بی‌خطر و تکرارپذیر.
-- نکتهٔ امنیتی: anon فقط SELECT می‌گیرد (قبلاً ALL بود)؛ امنیت ردیف‌ها همچنان با RLS است
--   و هر ۴۰ جدول RLS فعال دارند. جدول private.push_config بیرون از schema عمومی است و
--   هیچ گرنتی به anon/authenticated نمی‌گیرد.
-- ═══════════════════════════════════════════════════════════════════════════════

-- ۱) پیش‌فرض برای جدول‌ها/دنباله‌ها/توابعی که از این پس ساخته می‌شوند
alter default privileges for role postgres in schema public grant select on tables to anon;
alter default privileges for role postgres in schema public grant select, insert, update, delete on tables to authenticated;
alter default privileges for role postgres in schema public grant all on tables to service_role;
alter default privileges for role postgres in schema public grant usage, select on sequences to anon, authenticated, service_role;
alter default privileges for role postgres in schema public grant execute on functions to anon, authenticated, service_role;

-- ۱ـب) اگر مالک جدول‌ها supabase_admin باشد (در بعضی پروژه‌ها) همان پیش‌فرض‌ها برای او هم ست می‌شود.
--      اگر نقش وجود نداشته باشد، فقط یک notice می‌دهد و اجرا ادامه پیدا می‌کند.
do $$
begin
  begin
    execute 'alter default privileges for role supabase_admin in schema public grant select on tables to anon';
    execute 'alter default privileges for role supabase_admin in schema public grant select, insert, update, delete on tables to authenticated';
    execute 'alter default privileges for role supabase_admin in schema public grant all on tables to service_role';
    execute 'alter default privileges for role supabase_admin in schema public grant usage, select on sequences to anon, authenticated, service_role';
    execute 'alter default privileges for role supabase_admin in schema public grant execute on functions to anon, authenticated, service_role';
  exception when undefined_object or insufficient_privilege then
    raise notice 'نقش supabase_admin موجود یا قابل دسترس نبود — رد شد';
  end;
end $$;

-- ۲) گرنت صریح برای جدول‌های موجود
grant select on table public.app_version to anon;
grant select, insert, update, delete on table public.app_version to authenticated;
grant all on table public.app_version to service_role;

grant select on table public.bank_categories to anon;
grant select, insert, update, delete on table public.bank_categories to authenticated;
grant all on table public.bank_categories to service_role;

grant select on table public.bank_question_cats to anon;
grant select, insert, update, delete on table public.bank_question_cats to authenticated;
grant all on table public.bank_question_cats to service_role;

grant select on table public.calendar_note_classes to anon;
grant select, insert, update, delete on table public.calendar_note_classes to authenticated;
grant all on table public.calendar_note_classes to service_role;

grant select on table public.calendar_note_schools to anon;
grant select, insert, update, delete on table public.calendar_note_schools to authenticated;
grant all on table public.calendar_note_schools to service_role;

grant select on table public.calendar_note_students to anon;
grant select, insert, update, delete on table public.calendar_note_students to authenticated;
grant all on table public.calendar_note_students to service_role;

grant select on table public.calendar_notes to anon;
grant select, insert, update, delete on table public.calendar_notes to authenticated;
grant all on table public.calendar_notes to service_role;

grant select on table public.exam_audience_schools to anon;
grant select, insert, update, delete on table public.exam_audience_schools to authenticated;
grant all on table public.exam_audience_schools to service_role;

grant select on table public.exam_print_payments to anon;
grant select, insert, update, delete on table public.exam_print_payments to authenticated;
grant all on table public.exam_print_payments to service_role;

grant select on table public.feedback_bank to anon;
grant select, insert, update, delete on table public.feedback_bank to authenticated;
grant all on table public.feedback_bank to service_role;

grant select on table public.holiday_years to anon;
grant select, insert, update, delete on table public.holiday_years to authenticated;
grant all on table public.holiday_years to service_role;

grant select on table public.holidays to anon;
grant select, insert, update, delete on table public.holidays to authenticated;
grant all on table public.holidays to service_role;

grant select on table public.maintenance_audit to anon;
grant select, insert, update, delete on table public.maintenance_audit to authenticated;
grant all on table public.maintenance_audit to service_role;

grant select on table public.manager_approval_requests to anon;
grant select, insert, update, delete on table public.manager_approval_requests to authenticated;
grant all on table public.manager_approval_requests to service_role;

grant select on table public.manager_wallet_transfers_v38 to anon;
grant select, insert, update, delete on table public.manager_wallet_transfers_v38 to authenticated;
grant all on table public.manager_wallet_transfers_v38 to service_role;

grant select on table public.native_calendar_seen to anon;
grant select, insert, update, delete on table public.native_calendar_seen to authenticated;
grant all on table public.native_calendar_seen to service_role;

grant select on table public.native_exam_monitor to anon;
grant select, insert, update, delete on table public.native_exam_monitor to authenticated;
grant all on table public.native_exam_monitor to service_role;

grant select on table public.native_exam_operations to anon;
grant select, insert, update, delete on table public.native_exam_operations to authenticated;
grant all on table public.native_exam_operations to service_role;

grant select on table public.native_grade_progress to anon;
grant select, insert, update, delete on table public.native_grade_progress to authenticated;
grant all on table public.native_grade_progress to service_role;

grant select on table public.native_login_failures to anon;
grant select, insert, update, delete on table public.native_login_failures to authenticated;
grant all on table public.native_login_failures to service_role;

grant select on table public.native_registration_roles to anon;
grant select, insert, update, delete on table public.native_registration_roles to authenticated;
grant all on table public.native_registration_roles to service_role;

grant select on table public.native_restore_operations to anon;
grant select, insert, update, delete on table public.native_restore_operations to authenticated;
grant all on table public.native_restore_operations to service_role;

grant select on table public.native_staff_login_attempts to anon;
grant select, insert, update, delete on table public.native_staff_login_attempts to authenticated;
grant all on table public.native_staff_login_attempts to service_role;

grant select on table public.native_student_create_events to anon;
grant select, insert, update, delete on table public.native_student_create_events to authenticated;
grant all on table public.native_student_create_events to service_role;

grant select on table public.native_submission_operations to anon;
grant select, insert, update, delete on table public.native_submission_operations to authenticated;
grant all on table public.native_submission_operations to service_role;

grant select on table public.print_exam_payments to anon;
grant select, insert, update, delete on table public.print_exam_payments to authenticated;
grant all on table public.print_exam_payments to service_role;

grant select on table public.print_exams to anon;
grant select, insert, update, delete on table public.print_exams to authenticated;
grant all on table public.print_exams to service_role;

grant select on table public.push_outbox to anon;
grant select, insert, update, delete on table public.push_outbox to authenticated;
grant all on table public.push_outbox to service_role;

grant select on table public.push_tokens to anon;
grant select, insert, update, delete on table public.push_tokens to authenticated;
grant all on table public.push_tokens to service_role;

grant select on table public.school_admin_audit_v37 to anon;
grant select, insert, update, delete on table public.school_admin_audit_v37 to authenticated;
grant all on table public.school_admin_audit_v37 to service_role;

grant select on table public.school_invite_attempts_v39 to anon;
grant select, insert, update, delete on table public.school_invite_attempts_v39 to authenticated;
grant all on table public.school_invite_attempts_v39 to service_role;

grant select on table public.school_memberships to anon;
grant select, insert, update, delete on table public.school_memberships to authenticated;
grant all on table public.school_memberships to service_role;

grant select on table public.school_students to anon;
grant select, insert, update, delete on table public.school_students to authenticated;
grant all on table public.school_students to service_role;

grant select on table public.school_teacher_invites to anon;
grant select, insert, update, delete on table public.school_teacher_invites to authenticated;
grant all on table public.school_teacher_invites to service_role;

grant select on table public.schools to anon;
grant select, insert, update, delete on table public.schools to authenticated;
grant all on table public.schools to service_role;

grant select on table public.student_admin_audit to anon;
grant select, insert, update, delete on table public.student_admin_audit to authenticated;
grant all on table public.student_admin_audit to service_role;

grant select on table public.teacher_student_links to anon;
grant select, insert, update, delete on table public.teacher_student_links to authenticated;
grant all on table public.teacher_student_links to service_role;

grant select on table public.wallet_payment_orders to anon;
grant select, insert, update, delete on table public.wallet_payment_orders to authenticated;
grant all on table public.wallet_payment_orders to service_role;

grant select on table public.wallet_tx to anon;
grant select, insert, update, delete on table public.wallet_tx to authenticated;
grant all on table public.wallet_tx to service_role;

grant select on table public.wallets to anon;
grant select, insert, update, delete on table public.wallets to authenticated;
grant all on table public.wallets to service_role;

-- ۳) PostgREST کش schema را تازه کند
notify pgrst, 'reload schema';
