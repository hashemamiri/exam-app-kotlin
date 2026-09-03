package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.6 — توابع قدیمیِ خارج از ریپو (بند ۲.۳ گزارش امنیتی):
 * تا زمان انتقال تعریف این توابع به ریپو، دسترسیِ «پیش از ورود» (anon) از آن‌ها
 * گرفته می‌شود و ابزار استخراجِ تعریف‌شان در ریپو قرار می‌گیرد.
 */
class V75_6LegacyFunctionsAnonRevokeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(relative: String): String = File(root(), relative).readText()

    private val revoke by lazy { source("supabase/migrations/20260903_native_legacy_anon_revoke_v75_6.sql") }
    private val check by lazy { source("sql/manual/CHECK_LEGACY_FUNCTIONS_V75.sql") }
    private val doc by lazy { source("docs/fa/SECURITY_LEGACY_FUNCTIONS_V75.md") }

    @Test
    fun `anonymous access is revoked from the legacy functions`() {
        assertTrue("revoke execute on function public.%I(%s) from anon" in revoke)
        assertTrue("'get_exam_for_student'" in revoke)
        assertTrue("'submit_answer'" in revoke)
        assertTrue("'set_exam_audience'" in revoke)
    }

    @Test
    fun `teacher public profile stays public on purpose`() {
        assertFalse("'teacher_public_profile'" in revoke)
        assertTrue("teacher_public_profile" in doc)
    }

    @Test
    fun `the revocation loop is signature agnostic`() {
        assertTrue("pg_get_function_identity_arguments" in revoke)
        assertTrue("p.proname = any(v_names)" in revoke)
    }

    @Test
    fun `extraction queries are available for auditing`() {
        assertTrue("pg_get_functiondef" in check)
        assertTrue("get_exam_for_student" in check)
        assertTrue("submit_answer" in check)
    }
}
