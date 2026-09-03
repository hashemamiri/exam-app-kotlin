package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.2 — محدودسازی نرخِ native_staff_login_email_v1 (بند ۳.۱ گزارش امنیتی):
 * این تابع به anon مجاز است و ایمیل کامل معلم/مدیر را برمی‌گرداند؛ بدون محدودیت،
 * شمارش نام‌های کاربری و برداشت ایمیل آزاد بود. اکنون تلاش‌ها ثبت و محدود می‌شوند.
 */
class V75_2StaffLoginThrottleTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val throttle by lazy {
        File(root(), "supabase/migrations/20260903_native_staff_login_throttle_v75_2.sql").readText()
    }

    @Test
    fun `attempt table is created with row level security`() {
        assertTrue("create table if not exists public.native_staff_login_attempts" in throttle)
        assertTrue("alter table public.native_staff_login_attempts enable row level security" in throttle)
    }

    @Test
    fun `function counts and stores attempts before answering`() {
        val body = throttle.substringAfter("function public.native_staff_login_email_v1(").substringBefore("$$;")
        assertTrue("v_recent >= 5 or v_global >= 20" in body)
        assertTrue("insert into public.native_staff_login_attempts(username) values (v_username)" in body)
        assertTrue("attempted_at < now() - interval '2 hours'" in body)
    }

    @Test
    fun `function is no longer declared stable because it writes`() {
        val header = throttle.substringAfter("function public.native_staff_login_email_v1(").substringBefore("as $$")
        assertFalse("stable" in header)
        assertTrue("security definer" in header)
        assertTrue("set search_path = public, pg_temp" in header)
    }

    @Test
    fun `username login path keeps its grants`() {
        assertTrue("grant execute on function public.native_staff_login_email_v1(text) to anon, authenticated" in throttle)
    }
}
