package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.3 — محافظ ورودیِ مانیتورینگ آزمون (بند ۳.۲ گزارش امنیتی):
 * وجود آزمون، اندازهٔ هر گزارش، تعداد آیتم‌ها و سقف مجموعِ ذخیره‌شده.
 */
class V75_3ExamMonitorLimitsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val limits by lazy {
        File(root(), "supabase/migrations/20260903_native_exam_monitor_limits_v75_3.sql").readText()
    }

    private fun body(): String =
        limits.substringAfter("function public.native_monitor_upsert_v1(").substringBefore("$$;")

    @Test
    fun `unknown exams are rejected`() {
        assertTrue("select 1 from public.exams e where e.id = p_exam" in body())
        assertTrue("'آزمون یافت نشد'" in body())
    }

    @Test
    fun `report payload is bounded in size and items`() {
        assertTrue("octet_length(p_report::text)" in body())
        assertTrue("v_len > 8192" in body())
        assertTrue("jsonb_object_keys(p_report)" in body())
        assertTrue("v_keys > 100" in body())
    }

    @Test
    fun `merged report cannot grow forever`() {
        assertTrue("octet_length(coalesce(m.report::text, '{}'))" in body())
        assertTrue("coalesce(v_existing, 0) + v_len > 32768" in body())
    }

    @Test
    fun `monitor upload stays available to authenticated students`() {
        assertTrue("grant execute on function public.native_monitor_upsert_v1(text, jsonb) to authenticated" in limits)
    }
}
