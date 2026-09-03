package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.1 — گارد ارتقای نقش (بند ۲.۲ گزارش امنیتی):
 * ثبت‌نام مدیر/معلم فقط با ایمیل تأییدشده و فقط با نقشی که در ثبت‌نام انتخاب شده
 * انجام می‌شود؛ قبلاً این تصمیم کاملاً سمت کلاینت بود.
 */
class V75_1RegistrationRoleGuardTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val guard by lazy {
        File(root(), "supabase/migrations/20260903_native_registration_role_guard_v75_1.sql").readText()
    }

    private fun functionBody(name: String): String =
        guard.substringAfter("function public.$name(").substringBefore("$$;")

    @Test
    fun `manager registration requires a confirmed email`() {
        val body = functionBody("native_complete_manager_registration_v36")
        assertTrue("email_confirmed_at" in body)
        assertTrue("v_confirmed is null" in body)
    }

    @Test
    fun `manager registration requires the manager role chosen at signup`() {
        val body = functionBody("native_complete_manager_registration_v36")
        assertTrue("native_registration_roles" in body)
        assertTrue("v_pending" in body)
        assertTrue("to_regclass('public.native_registration_roles')" in body)
        assertTrue("coalesce(v_pending, 'teacher') <> 'manager'" in body)
    }

    @Test
    fun `teacher registration also requires a confirmed email`() {
        val body = functionBody("native_complete_teacher_registration_v1")
        assertTrue("email_confirmed_at" in body)
        assertTrue("v_confirmed is null" in body)
    }

    @Test
    fun `existing ownership and uniqueness checks survive`() {
        val body = functionBody("native_complete_manager_registration_v36")
        assertTrue("v_profile.teacher_id is not null" in body)
        assertTrue("pg_advisory_xact_lock" in body)
        assertTrue("این نام کاربری قبلاً استفاده شده است" in body)
        assertTrue("grant execute on function public.native_complete_teacher_registration_v1(text,text) to authenticated" in guard)
    }
}
