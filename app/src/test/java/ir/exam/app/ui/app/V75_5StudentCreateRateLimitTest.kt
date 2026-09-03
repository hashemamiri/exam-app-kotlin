package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.5 — محدودیت نرخِ ساخت حساب دانش‌آموز (بند ۳.۶ گزارش امنیتی):
 * عمل create و bulk-create در Edge Function مدیریت دانش‌آموز سهمیهٔ ساعتی/روزانه
 * می‌گیرند؛ شمارنده در دیتابیس و با قفل مشورتی است تا با ری‌استارت Edge صفر نشود.
 */
class V75_5StudentCreateRateLimitTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val quota by lazy {
        File(root(), "supabase/migrations/20260903_native_student_create_quota_v75_5.sql").readText()
    }

    private val edge by lazy {
        File(root(), "supabase/functions/manage-student/index.ts").readText()
    }

    @Test
    fun `quota is counted in the database under an advisory lock`() {
        assertTrue("create table if not exists public.native_student_create_events" in quota)
        assertTrue("pg_advisory_xact_lock" in quota)
        assertTrue("enable row level security" in quota)
    }

    @Test
    fun `quota function is only callable by the service role`() {
        assertTrue("security definer" in quota)
        assertTrue("from public, anon, authenticated" in quota)
        assertTrue("to service_role" in quota)
    }

    @Test
    fun `single create consumes one unit`() {
        assertTrue("consumeCreateQuota(1)" in edge)
    }

    @Test
    fun `bulk create consumes one unit per row`() {
        assertTrue("consumeCreateQuota(rows.length)" in edge)
    }

    @Test
    fun `quota rejection answers with too many requests`() {
        assertTrue("429" in edge.substringAfter("consumeCreateQuota(1)"))
        assertTrue("native_consume_student_create_quota" in edge)
    }
}
