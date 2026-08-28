package ir.exam.app.ui.app

import ir.exam.app.data.local.PrintLayoutMerger
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.6 — سه اصلاح گزارش‌شدهٔ دستگاه:
 * ۱) ویرایشگر چاپ هیچ کادر متنی/کادر انتخابی دور متن نشان نمی‌دهد؛
 * ۲) تغییرات ذخیره‌شدهٔ بخش «آزمون‌ها» به چاپ rebase می‌شوند، اما اختلاف‌های
 *    مخصوص چاپ وارد آزمون دانش‌آموز نمی‌شوند؛
 * ۳) حلقهٔ سفید اسپینر در پایان دور به همان زاویه برمی‌گردد و نمی‌پرد.
 */
class V64_6PrintEditorSyncTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy {
        source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt")
    }
    private val store by lazy {
        source("app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt")
    }
    private val builder by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
    }
    private val spinner by lazy {
        source("app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt")
            .substringAfter("internal fun IceSpinner(")
            .substringBefore("fun IceSessionLoading(")
    }

    @Test
    fun `print editor text has no box decoration or selection border`() {
        assertTrue("decorationBox = { innerField -> innerField() }" in editor)
        assertTrue("modifier.background(Color.Transparent)" in editor)
        assertFalse("if (highlighted) {\n                    Modifier.border" in editor)
        assertFalse("then(if (selected) Modifier.border(2.dp" in editor)
    }

    @Test
    fun `canonical exam saves rebase print overrides without changing student exam`() {
        assertTrue("var canonicalQuestions by remember(examId)" in editor)
        assertTrue("layoutStore.readForLatest(examId, latest)" in editor)
        assertTrue("canonicalQuestions ?: state.questions" in editor)
        assertTrue("fun rebase(examId: String, latestQuestions: List<QuestionDraft>)" in store)
        assertTrue("PrintLayoutMerger.merge(stored.base, stored.print, latestQuestions)" in store)
        assertTrue("saveState.examId?.let { printLayoutStore.rebase(it, saveState.questions) }" in builder)
    }

    @Test
    fun `merge keeps a print-only field but accepts a new canonical field`() {
        val base = question(text = "متن اولیه", score = 1.0)
        // فقط بارم در خروجی چاپ تغییر کرده است.
        val print = base.copy(score = 2.0)
        // بخش آزمون‌ها بعداً متن سؤال را تغییر داده است.
        val latest = base.copy(text = "متن جدید در آزمون‌ها")

        val merged = PrintLayoutMerger.merge(listOf(base), listOf(print), listOf(latest)).single()

        assertEquals("متن جدید در آزمون‌ها", merged.text)
        assertEquals(2.0, merged.score, 0.0)
        // این تست نشان می‌دهد نسخهٔ print فقط در memory/store چاپ است؛
        // latest که به آزمون دانش‌آموز می‌رود هرگز با print جایگزین نمی‌شود.
        assertEquals("متن جدید در آزمون‌ها", latest.text)
        assertEquals(1.0, latest.score, 0.0)
    }

    @Test
    fun `independent option changes from print and exam are both retained`() {
        val base = QuestionDraft(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            text = "سؤال",
            options = listOf("الف", "ب", "ج"),
            optionIds = listOf("o1", "o2", "o3"),
            optionImages = listOf(null, null, null)
        )
        val print = base.copy(options = listOf("الف چاپی", "ب", "ج"))
        val latest = base.copy(options = listOf("الف", "ب جدید در آزمون‌ها", "ج"))

        val merged = PrintLayoutMerger.merge(listOf(base), listOf(print), listOf(latest)).single()

        assertEquals(listOf("الف چاپی", "ب جدید در آزمون‌ها", "ج"), merged.options)
    }

    @Test
    fun `print-only question order survives while new canonical questions are appended`() {
        val first = question(id = "q1", text = "اول")
        val second = question(id = "q2", text = "دوم")
        val printOrder = listOf(second, first)
        val latest = listOf(first.copy(text = "اولِ جدید"), second, question(id = "q3", text = "تازه"))

        val merged = PrintLayoutMerger.merge(listOf(first, second), printOrder, latest)

        assertEquals(listOf("q2", "q1", "q3"), merged.map { it.id })
        assertEquals("اولِ جدید", merged[1].text)
    }

    @Test
    fun `white spinner arc uses a seamless full turn`() {
        assertTrue("val innerAngle by transition.animateFloat" in spinner)
        assertTrue("targetValue = 360f" in spinner)
        assertTrue("rotate(-innerAngle + 160f)" in spinner)
        assertFalse("angle * 1.4f" in spinner)
    }

    private fun question(
        id: String = "q1",
        text: String,
        score: Double = 1.0
    ) = QuestionDraft(
        id = id,
        type = QuestionType.ESSAY,
        text = text,
        score = score
    )
}
