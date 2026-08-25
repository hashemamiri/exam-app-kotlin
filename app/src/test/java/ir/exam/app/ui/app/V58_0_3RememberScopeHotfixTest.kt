package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V58.0.3 — گزارش CI پس از V58.0.2:
 * «StudentExamScreen.kt:401: @Composable invocations can only happen from
 *  the context of a @Composable function»
 *
 * ریشه: V58.0.2 محاسبهٔ questionHasGraph (با remember که @Composable است)
 * را مستقیم داخل بدنهٔ LazyColumn گذاشته بود؛ آن بدنه LazyListScope است نه
 * Composable و فقط داخل item {} می‌توان Composable صدا زد.
 *
 * راه‌حل: انتقال remember به بدنهٔ StudentExamContent (کنار محاسبهٔ
 * question/presentation، قبل از Scaffold) — رفتار بدون تغییر.
 */
class V58_0_3RememberScopeHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val student by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt").readText()
    }

    @Test
    fun `question graph detection lives in composable scope before the scaffold`() {
        val declaration = student.indexOf("val questionHasGraph = remember(question.id, question.text)")
        val scaffold = student.indexOf("    Scaffold(")
        assertTrue("questionHasGraph declaration missing", declaration >= 0)
        assertTrue("Scaffold missing", scaffold >= 0)
        // اعلان قبل از Scaffold یعنی در متن Composable، نه داخل LazyListScope.
        assertTrue("remember must run in composable scope, not LazyListScope", declaration < scaffold)
        // مصرف در همان جای قبلی است.
        assertTrue("if (presentation.allowAnswerGraph || questionHasGraph)" in student)
    }

    @Test
    fun `no remember call leaks directly into the lazy list scope`() {
        val lazyBody = student.substringAfter("LazyColumn(")
        var inItem = 0
        lazyBody.lines().forEach { line ->
            val s = line.trim()
            if (s.startsWith("item {") || s.startsWith("item(")) inItem++
            if (inItem == 0 && s.startsWith("val ") && "remember(" in s) {
                throw AssertionError("remember outside item{} in LazyListScope: $s")
            }
        }
        assertTrue(true)
    }
}
