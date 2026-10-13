package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V270 — شش مورد گزارش QA (اپ + SQL): نهایی‌سازی نمره با پیام روشن، اشتراک کلاس هنگام ساخت، نام پایه. */
class V270_GradeFlowsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `sql finalize and approve report essay questions`() {
        val sql = src("supabase/migrations/20261013_native_finalize_grades_v270.sql")
        assertTrue("create or replace function public.native_finalize_bulk_grades_v1(p_exam text)" in sql)
        assertTrue("create or replace function public.approve_auto_grades(p_exam text, p_mode text default 'auto_only')" in sql)
        assertTrue("'essay_questions', to_jsonb(v_essay_q)" in sql)
    }

    @Test
    fun `app shows essay questions left and shares class on create`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        assertTrue("essayQuestions = (raw[\"essay_questions\"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()" in repo)
        val school = src("app/src/main/java/ir/exam/app/domain/repository/SchoolRepository.kt")
        assertTrue("suspend fun createClassReturningId(name: String, grade: String, fieldOfStudy: String = \"\"): Result<String> =" in school)
        val vm = src("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt")
        assertTrue("val newId = repository.createClassReturningId(name, grade, fieldOfStudy).getOrThrow()" in vm)
        assertTrue("error(\"کلاس ساخته شد؛ اما اشتراک با مدیر انجام نشد: \${e.message.orEmpty()}\")" in vm)
        val scr = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("Text(\"اشتراک با مدیر مدرسه (در کارنامهٔ مدیر دیده شود)\", style = MaterialTheme.typography.bodySmall)" in scr)
    }
}
