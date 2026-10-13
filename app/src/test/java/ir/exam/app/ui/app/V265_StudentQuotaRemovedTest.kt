package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V265 — حذف کامل محدودیت ساخت دانش‌آموز: سهمیهٔ ساعتی/روزانهٔ V75.5 روی سرور خنثی شد،
 * سقف ۱۰۰ ردیفِ اپ به سقف نرمِ ۱۰۰۰ تبدیل شد و مخزن درخواست‌ها را دسته‌دسته (۱۰۰تایی) می‌فرستد.
 * (پین‌های سایت در این فایل نیست.)
 */
class V265_StudentQuotaRemovedTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `server migration makes quota unlimited`() {
        val sql = src("supabase/migrations/20261013_native_student_quota_removed_v265.sql")
        assertTrue(sql.contains("native_consume_student_create_quota"))
        assertTrue(sql.contains("'unlimited', true"))
        assertTrue(sql.contains("remaining_hour"))
    }

    @Test
    fun `repository sends bulk in chunks without size cap`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt")
        assertTrue(repo.contains("requests.chunked(100)"))
        assertFalse(repo.contains("requests.size in 1..100"))
    }

    @Test
    fun `screen uses soft bulk cap constant`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue(screen.contains("private const val BULK_STUDENT_MAX = 1000"))
        assertTrue(screen.indexOf("BULK_STUDENT_MAX = 1000") < screen.indexOf("fun generatePassword"))
        assertFalse(screen.contains("rows.size < 100"))
    }
}
