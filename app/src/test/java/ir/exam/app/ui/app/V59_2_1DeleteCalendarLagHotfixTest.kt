package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V59.2.1 — سه گزارش دستگاه:
 * ۱) «حذف حساب اصلی ناموفق بود؛ دانش‌آموزان پردازش شدند»: ریشه = FKهای بدون
 *    cascade به auth.users حذف ردیف معلم/مدیر را بلاک می‌کردند
 *    (schools.created_by و school_students.created_by با on delete restrict؛
 *    invites/audit/transfers بدون قاعده). SQL آماده‌سازی حالا همه را
 *    پاک/منتقل می‌کند و Edge علت دقیق خطای auth را برمی‌گرداند.
 * ۲) «پیام تقویم برای دانش‌آموز نمایش داده نمی‌شود» (ادامه): پوشش دید علاوه
 *    بر مالک و لینک، «معلمِ کلاسی که دانش‌آموز عضو آن است» را هم گرفت —
 *    سناریوی دانش‌آموزِ ساختهٔ مدیر که فقط عضو کلاس معلم است.
 * ۳) «کادر متن سؤال با تاخیر/پرش باز می‌شود»: دو انیمیشن تو در تو
 *    (expandVertically بیرونی + animateContentSize داخلی) روی ارتفاع متغیر
 *    WebView (۱۵۰dp → ارتفاع واقعی بعد از لود HTML) پرش می‌ساخت؛
 *    animateContentSize حذف و WebView تا اولین گزارش ارتفاع محو (alpha=0)
 *    نگه داشته می‌شود.
 */
class V59_2_1DeleteCalendarLagHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val deleteSql by lazy { source("supabase/migrations/20260825_native_delete_account_v59.sql") }
    private val calSql by lazy { source("supabase/migrations/20260825_native_calendar_notify_v59.sql") }
    private val edge by lazy { source("supabase/functions/manage-student/index.ts") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }

    @Test
    fun `account deletion clears every non-cascading auth reference`() {
        assertTrue("delete from public.school_teacher_invites where created_by = v_uid" in deleteSql)
        assertTrue("delete from public.school_admin_audit_v37 where actor_id = v_uid or target_id = v_uid" in deleteSql)
        assertTrue("delete from public.manager_wallet_transfers_v38 where manager_id = v_uid or teacher_id = v_uid" in deleteSql)
        assertTrue("delete from public.manager_approval_requests where manager_id = v_uid or teacher_id = v_uid" in deleteSql)
        assertTrue("delete from public.school_students where created_by = v_uid" in deleteSql)
        // مدرسهٔ مدیر: انتقال به مدیر فعال دیگر وگرنه حذف
        assertTrue("update public.schools s" in deleteSql)
        assertTrue("delete from public.schools where created_by = v_uid" in deleteSql)
        // Edge علت دقیق را برمی‌گرداند
        assertTrue("حذف حساب اصلی ناموفق بود: " in edge)
    }

    @Test
    fun `calendar visibility also covers class-membership teachers`() {
        val calMonth = calSql.substringAfter("create or replace function public.cal_month")
            .substringBefore("create or replace function public.cal_save_note")
        assertTrue("join public.classes c on c.id = m.class_id" in calMonth)
        assertTrue("where m.student_id = v_uid and c.teacher_id = n.teacher_id" in calMonth)
        val unseen = calSql.substringAfter("create or replace function public.cal_unseen_v59")
        assertTrue("join public.classes c on c.id = m.class_id" in unseen)
    }

    @Test
    fun `question text field opens without nested animations or flashes`() {
        assertFalse("animateContentSize" in webSection)
        assertTrue("var webReady by remember { mutableStateOf(false) }" in webSection)
        assertTrue("webReady = true" in webSection)
        assertTrue("alpha = if (webReady || overlayOpen) 1f else 0f" in webSection)
    }
}
